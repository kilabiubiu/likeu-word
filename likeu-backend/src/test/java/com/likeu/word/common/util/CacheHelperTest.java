package com.likeu.word.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缓存读写单测：TTL 抖动、空值短 TTL、反序列化失败回源、Redis 故障降级、单飞重建
 */
class CacheHelperTest {

    private FakeRedisUtil redis;

    private CacheHelper cacheHelper;

    @BeforeEach
    void setUp() {
        redis = new FakeRedisUtil();
        cacheHelper = new CacheHelper();
        ReflectionTestUtils.setField(cacheHelper, "redisUtil", redis);
    }

    @Test
    @DisplayName("未命中时回源并写缓存，命中后不再回源")
    void loadsOnceThenServesFromCache() {
        AtomicInteger loads = new AtomicInteger();

        Sample first = cacheHelper.get("key", Sample.class, 3600, () -> {
            loads.incrementAndGet();
            return new Sample(1L, "cached");
        });
        Sample second = cacheHelper.get("key", Sample.class, 3600, () -> {
            loads.incrementAndGet();
            return new Sample(2L, "loaded-again");
        });

        assertEquals(1, loads.get());
        assertEquals("cached", first.getName());
        assertEquals("cached", second.getName());
        assertEquals(1L, second.getId());
    }

    @Test
    @DisplayName("TTL 带 ±10% 抖动，避免同批 key 同时过期")
    void ttlIsJitteredWithinTenPercent() {
        for (int i = 0; i < 20; i++) {
            cacheHelper.get("key" + i, Sample.class, 3600, () -> new Sample(1L, "v"));
        }

        for (Map.Entry<String, Long> entry : redis.ttlSeconds.entrySet()) {
            long ttl = entry.getValue();
            assertTrue(ttl >= 3240 && ttl <= 3960, "TTL 超出 ±10% 区间: " + ttl);
        }
    }

    @Test
    @DisplayName("对象为空时按 60 秒短 TTL 缓存占位，不再穿透到数据库")
    void nullValueIsCachedWithShortTtl() {
        AtomicInteger loads = new AtomicInteger();

        Sample first = cacheHelper.get("missing", Sample.class, 3600, () -> {
            loads.incrementAndGet();
            return null;
        });
        Sample second = cacheHelper.get("missing", Sample.class, 3600, () -> {
            loads.incrementAndGet();
            return null;
        });

        assertNull(first);
        assertNull(second);
        assertEquals(1, loads.get());
        assertEquals("@null@", redis.store.get("missing"));
        assertEquals(60L, redis.ttlSeconds.get("missing"));
    }

    @Test
    @DisplayName("空列表按 60 秒短 TTL 缓存，避免不存在的 id 反复查库")
    void emptyListIsCachedWithShortTtl() {
        AtomicInteger loads = new AtomicInteger();

        List<Sample> first = cacheHelper.getList("empty", Sample.class, 3600, () -> {
            loads.incrementAndGet();
            return Collections.emptyList();
        });
        List<Sample> second = cacheHelper.getList("empty", Sample.class, 3600, () -> {
            loads.incrementAndGet();
            return Collections.emptyList();
        });

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertEquals(1, loads.get());
        assertEquals("[]", redis.store.get("empty"));
        assertEquals(60L, redis.ttlSeconds.get("empty"));
    }

    @Test
    @DisplayName("列表缓存往返序列化后内容一致")
    void listRoundTripKeepsContent() {
        AtomicInteger loads = new AtomicInteger();
        cacheHelper.getList("list", Sample.class, 600,
                () -> Arrays.asList(new Sample(1L, "a"), new Sample(2L, "b")));

        List<Sample> cached = cacheHelper.getList("list", Sample.class, 600, () -> {
            loads.incrementAndGet();
            return Collections.emptyList();
        });

        assertEquals(0, loads.get());
        assertEquals(2, cached.size());
        assertEquals("a", cached.get(0).getName());
        assertEquals(2L, cached.get(1).getId());
    }

    @Test
    @DisplayName("缓存内容损坏时删除该 key 并回源数据库")
    void brokenCacheFallsBackToLoader() {
        redis.store.put("broken", "{");

        Sample value = cacheHelper.get("broken", Sample.class, 60, () -> new Sample(9L, "reloaded"));

        assertEquals("reloaded", value.getName());
        assertFalse(redis.store.containsKey("broken") && "{".equals(redis.store.get("broken")));
    }

    @Test
    @DisplayName("Redis 读故障时降级为直接回源，不影响主流程")
    void redisGetFailureDegradesToLoader() {
        redis.failGet = true;

        Sample value = cacheHelper.get("key", Sample.class, 60, () -> new Sample(3L, "from-db"));

        assertEquals("from-db", value.getName());
    }

    @Test
    @DisplayName("Redis 写故障时仍返回回源结果")
    void redisSetFailureStillReturnsValue() {
        redis.failSet = true;

        Sample value = cacheHelper.get("key", Sample.class, 60, () -> new Sample(4L, "from-db"));

        assertEquals("from-db", value.getName());
    }

    @Test
    @DisplayName("缓存删除失败仅告警，不抛异常")
    void evictSwallowsRedisFailure() {
        redis.failDelete = true;

        cacheHelper.evict("key");

        assertTrue(redis.store.isEmpty());
    }

    @Test
    @DisplayName("evict 会真正删除缓存")
    void evictRemovesKey() {
        cacheHelper.get("key", Sample.class, 60, () -> new Sample(5L, "v"));

        cacheHelper.evict("key");

        assertNull(redis.store.get("key"));
    }

    @Test
    @DisplayName("并发重建同一 key 时只回源一次（单飞）")
    void concurrentRebuildLoadsOnlyOnce() throws Exception {
        int threads = 8;
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Sample value = cacheHelper.get("hot-key", Sample.class, 600, () -> {
                        loads.incrementAndGet();
                        sleep(80);
                        return new Sample(1L, "loaded");
                    });
                    assertNotNull(value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, loads.get(), "并发重建应只回源一次");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 测试用 POJO：Hutool JSON 需要无参构造 + 标准 getter/setter */
    public static class Sample {

        private Long id;

        private String name;

        public Sample() {
        }

        Sample(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /** 内存版 RedisUtil，避免单测依赖真实 Redis */
    private static class FakeRedisUtil extends RedisUtil {

        final Map<String, String> store = new ConcurrentHashMap<>();

        final Map<String, Long> ttlSeconds = new ConcurrentHashMap<>();

        boolean failGet;

        boolean failSet;

        boolean failDelete;

        @Override
        public void set(String key, String value, long timeout, TimeUnit unit) {
            if (failSet) {
                throw new IllegalStateException("redis unavailable");
            }
            store.put(key, value);
            ttlSeconds.put(key, unit.toSeconds(timeout));
        }

        @Override
        public String get(String key) {
            if (failGet) {
                throw new IllegalStateException("redis unavailable");
            }
            return store.get(key);
        }

        @Override
        public Boolean delete(String key) {
            if (failDelete) {
                throw new IllegalStateException("redis unavailable");
            }
            store.remove(key);
            ttlSeconds.remove(key);
            return true;
        }
    }
}
