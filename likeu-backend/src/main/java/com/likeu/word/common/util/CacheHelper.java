package com.likeu.word.common.util;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 基础数据 JSON 缓存读写助手（进程内实现）
 *
 * <p>词根、词书、单词等低频变更的数据统一走这里，集中处理三件事：</p>
 * <ol>
 *   <li><b>TTL 抖动</b>：实际 TTL 取传入值的 ±10%，避免同批 key 同时过期引发集中回源</li>
 *   <li><b>单飞重建</b>：同一 key 同一时刻只允许一个线程回源，其余线程等锁后复用结果</li>
 *   <li><b>空值短 TTL</b>：null / 空列表按 {@value #EMPTY_TTL_SECONDS} 秒缓存，避免不存在的 id 直接穿透到数据库</li>
 * </ol>
 *
 * <p><b>为什么不是 Redis：</b>微信云托管明确不支持部署数据库 / Redis 等有状态服务，
 * 而本项目的缓存只是「可随时失效的加速层」，因此改为进程内缓存，
 * 少一个必须外购并打通 VPC 的中间件。代价与约束：</p>
 * <ul>
 *   <li>每个实例各有一份缓存，扩缩容 / 重新发布后缓存自然重建，不需要预热；</li>
 *   <li>{@link #evict(String)} 只清理当前实例，运维侧改动基础数据后如需立刻全量生效，
 *       请重新发布版本（或在控制台重启服务），否则最迟等 TTL 到期。</li>
 * </ul>
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

    /** 空值在缓存中的占位内容：Map 无法区分「未缓存」与「已缓存为空」，用该字面量区分 */
    private static final String NULL_PLACEHOLDER = "@null@";

    /** 分段锁个数：固定长度，避免锁对象随 key 数量无界增长 */
    private static final int LOCK_SEGMENTS = 64;
    private static final Object[] LOCKS = new Object[LOCK_SEGMENTS];

    /**
     * 条目数上限：容器内存有限，超过后先清理已过期条目，仍超限则整体清空。
     * 正常业务下 key 总量（词书 + 词根 + 单词详情）远小于该值，只有异常流量才会触发。
     */
    private static final int MAX_ENTRIES = 5000;

    static {
        for (int i = 0; i < LOCK_SEGMENTS; i++) {
            LOCKS[i] = new Object();
        }
    }

    /** 缓存条目：值 + 过期时间戳（毫秒） */
    private static class Entry {

        final String value;
        final long expireAtMillis;

        Entry(String value, long expireAtMillis) {
            this.value = value;
            this.expireAtMillis = expireAtMillis;
        }

        boolean expired(long nowMillis) {
            return nowMillis > expireAtMillis;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

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

    /** 数据变更后主动删除缓存（只影响当前实例，多实例下由 TTL 兜底） */
    public void evict(String key) {
        store.remove(key);
    }

    private <T> Hit<T> readBean(String key, Class<T> type) {
        String cached = read(key);
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
            store.remove(key);
            return Hit.miss();
        }
    }

    private <T> Hit<List<T>> readList(String key, Class<T> elementType) {
        String cached = read(key);
        if (cached == null) {
            return Hit.miss();
        }
        try {
            List<T> list = JSONUtil.toList(cached, elementType);
            return Hit.of(list == null ? new ArrayList<>() : list);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，将回源数据库: key={}", key, e);
            store.remove(key);
            return Hit.miss();
        }
    }

    /** 读取并顺带惰性清理过期条目，返回 null 表示未命中 */
    private String read(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired(System.currentTimeMillis())) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    private void write(String key, String value, long ttlSeconds) {
        store.put(key, new Entry(value, System.currentTimeMillis() + Math.max(ttlSeconds, 1) * 1000));
        guardCapacity();
    }

    /** 容量保护：先清理过期条目，仍超上限则整体清空，保证内存不会被异常流量撑爆 */
    private void guardCapacity() {
        if (store.size() <= MAX_ENTRIES) {
            return;
        }
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expired(now));
        if (store.size() > MAX_ENTRIES) {
            log.warn("缓存条目超过上限 {}，整体清空以避免内存膨胀", MAX_ENTRIES);
            store.clear();
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

    // ============== 以下方法仅供同包单测断言缓存内部状态，业务代码不要调用 ==============

    /** 当前缓存条目数（含未清理的过期条目） */
    int cachedKeyCount() {
        return store.size();
    }

    /** 剩余 TTL（秒）；-1 表示 key 不存在 */
    long remainingTtlSeconds(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return -1L;
        }
        return (entry.expireAtMillis - System.currentTimeMillis()) / 1000;
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
