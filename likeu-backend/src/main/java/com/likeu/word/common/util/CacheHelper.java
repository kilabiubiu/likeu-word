package com.likeu.word.common.util;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基础数据 JSON 缓存读写助手
 *
 * <p>词根、词书、单词等低频变更的数据统一走这里，集中处理三件事：</p>
 * <ol>
 *   <li><b>TTL 抖动</b>：实际 TTL 取传入值的 ±10%，避免同批 key 同时过期引发集中回源</li>
 *   <li><b>单飞重建</b>：同一 key 同一时刻只允许一个线程回源，其余线程等锁后复用结果</li>
 *   <li><b>空值短 TTL</b>：null / 空列表按 {@value #EMPTY_TTL_SECONDS} 秒缓存，避免不存在的 id 直接穿透到数据库</li>
 * </ol>
 *
 * <p>缓存被当作「可随时失效的加速层」：读失败或反序列化失败一律降级为回源数据库，不影响主流程。</p>
 */
@Slf4j
@Component
public class CacheHelper {

    /** 空值缓存时长（秒）：短 TTL 兼顾防穿透与数据及时可见 */
    private static final long EMPTY_TTL_SECONDS = 60;

    /** TTL 抖动比例：±10% */
    private static final double JITTER_RATIO = 0.1;

    /** 空值在缓存中的占位内容：Redis 无法存 null，用该字面量区分「未缓存」与「已缓存为空」 */
    private static final String NULL_PLACEHOLDER = "@null@";

    /** 分段锁个数：固定长度，避免锁对象随 key 数量无界增长 */
    private static final int LOCK_SEGMENTS = 64;
    private static final Object[] LOCKS = new Object[LOCK_SEGMENTS];

    static {
        for (int i = 0; i < LOCK_SEGMENTS; i++) {
            LOCKS[i] = new Object();
        }
    }

    @Resource
    private RedisUtil redisUtil;

    /**
     * 读取单个对象，未命中时回源并写入缓存
     *
     * @param ttlSeconds 缓存时长（秒），实际写入时带 ±10% 抖动
     */
    public <T> T get(String key, Class<T> type, long ttlSeconds, Supplier<T> loader) {
        Hit<T> hit = readBean(key, type);
        if (hit.found) {
            return hit.value;
        }
        synchronized (lockOf(key)) {
            // 双重检查：等锁期间可能已有其他线程完成重建
            hit = readBean(key, type);
            if (hit.found) {
                return hit.value;
            }
            T value = loader.get();
            if (value == null) {
                write(key, NULL_PLACEHOLDER, EMPTY_TTL_SECONDS);
            } else {
                write(key, JSONUtil.toJsonStr(value), jitterTtl(ttlSeconds));
            }
            return value;
        }
    }

    /**
     * 读取列表，未命中时回源并写入缓存
     *
     * @param ttlSeconds 缓存时长（秒），实际写入时带 ±10% 抖动；空列表统一使用空值短 TTL
     */
    public <T> List<T> getList(String key, Class<T> elementType, long ttlSeconds, Supplier<List<T>> loader) {
        Hit<List<T>> hit = readList(key, elementType);
        if (hit.found) {
            return hit.value;
        }
        synchronized (lockOf(key)) {
            hit = readList(key, elementType);
            if (hit.found) {
                return hit.value;
            }
            List<T> value = loader.get();
            if (value == null || value.isEmpty()) {
                write(key, "[]", EMPTY_TTL_SECONDS);
                return value == null ? new ArrayList<>() : value;
            }
            write(key, JSONUtil.toJsonStr(value), jitterTtl(ttlSeconds));
            return value;
        }
    }

    /** 数据变更后主动删除缓存；删除失败仅告警，一致性由 TTL 兜底 */
    public void evict(String key) {
        try {
            redisUtil.delete(key);
        } catch (Exception e) {
            log.warn("缓存删除失败，将由 TTL 到期兜底恢复一致: key={}", key, e);
        }
    }

    private <T> Hit<T> readBean(String key, Class<T> type) {
        String cached = safeGet(key);
        if (cached == null) {
            return Hit.miss();
        }
        if (NULL_PLACEHOLDER.equals(cached)) {
            return Hit.of(null);
        }
        try {
            return Hit.of(JSONUtil.toBean(cached, type));
        } catch (Exception e) {
            log.warn("缓存反序列化失败，将回源数据库: key={}", key, e);
            redisUtil.delete(key);
            return Hit.miss();
        }
    }

    private <T> Hit<List<T>> readList(String key, Class<T> elementType) {
        String cached = safeGet(key);
        if (cached == null) {
            return Hit.miss();
        }
        try {
            List<T> list = JSONUtil.toList(cached, elementType);
            return Hit.of(list == null ? new ArrayList<>() : list);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，将回源数据库: key={}", key, e);
            redisUtil.delete(key);
            return Hit.miss();
        }
    }

    private String safeGet(String key) {
        try {
            return redisUtil.get(key);
        } catch (Exception e) {
            log.warn("缓存读取失败，将回源数据库: key={}", key, e);
            return null;
        }
    }

    private void write(String key, String value, long ttlSeconds) {
        try {
            redisUtil.set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存写入失败，本次仍返回回源结果: key={}", key, e);
        }
    }

    /** 在传入 TTL 上叠加 ±10% 抖动，防止同批 key 同时过期 */
    private long jitterTtl(long ttlSeconds) {
        long delta = (long) (ttlSeconds * JITTER_RATIO);
        if (delta <= 0) {
            return Math.max(ttlSeconds, 1);
        }
        return ttlSeconds - delta + ThreadLocalRandom.current().nextLong(delta * 2 + 1);
    }

    private Object lockOf(String key) {
        return LOCKS[(key.hashCode() & 0x7fffffff) % LOCK_SEGMENTS];
    }

    /** 缓存查询结果：found=false 表示未命中（需要回源），found=true 时 value 可能为 null（已缓存空值） */
    private static class Hit<T> {

        final boolean found;
        final T value;

        private Hit(boolean found, T value) {
            this.found = found;
            this.value = value;
        }

        static <T> Hit<T> miss() {
            return new Hit<>(false, null);
        }

        static <T> Hit<T> of(T value) {
            return new Hit<>(true, value);
        }
    }
}
