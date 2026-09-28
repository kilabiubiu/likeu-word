package com.likeu.word.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缓存读写单测：TTL 抖动、空值短 TTL、内容不兼容时回源、主动失效、单飞重建、容量保护
 */
class CacheHelperTest {

    private CacheHelper cacheHelper;

    @BeforeEach
    void setUp() {
        cacheHelper = new CacheHelper();
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
            String key = "key" + i;
            cacheHelper.get(key, Sample.class, 3600, () -> new Sample(1L, "v"));

            long ttl = cacheHelper.remainingTtlSeconds(key);
            assertTrue(ttl >= 3239 && ttl <= 3960, "TTL 超出 ±10% 区间: " + ttl);
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
        assertShortTtl(cacheHelper.remainingTtlSeconds("missing"));
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
        assertShortTtl(cacheHelper.remainingTtlSeconds("empty"));
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
    @DisplayName("缓存内容与目标类型不兼容时丢弃缓存并回源数据库")
    void incompatibleCacheFallsBackToLoader() {
        // 先按对象写入，再按列表读取：反序列化必然失败，应丢弃并回源
        cacheHelper.get("mixed", Sample.class, 600, () -> new Sample(1L, "v"));

        AtomicInteger loads = new AtomicInteger();
        List<Sample> reloaded = cacheHelper.getList("mixed", Sample.class, 600, () -> {
            loads.incrementAndGet();
            return Collections.singletonList(new Sample(9L, "reloaded"));
        });

        assertEquals(1, loads.get());
        assertEquals(9L, reloaded.get(0).getId());
    }

    @Test
    @DisplayName("evict 会真正删除缓存并触发下次回源")
    void evictRemovesKey() {
        cacheHelper.get("key", Sample.class, 60, () -> new Sample(5L, "v"));

        cacheHelper.evict("key");

        assertEquals(-1L, cacheHelper.remainingTtlSeconds("key"));
        AtomicInteger loads = new AtomicInteger();
        cacheHelper.get("key", Sample.class, 60, () -> {
            loads.incrementAndGet();
            return new Sample(6L, "again");
        });
        assertEquals(1, loads.get());
    }

    @Test
    @DisplayName("过期条目在读取时被惰性清理")
    void expiredEntryIsDropped() throws Exception {
        cacheHelper.get("ttl", Sample.class, 1, () -> new Sample(1L, "v"));
        assertNotNull(cacheHelper.get("ttl", Sample.class, 1, () -> new Sample(2L, "v2")));

        Thread.sleep(1100);

        AtomicInteger loads = new AtomicInteger();
        Sample value = cacheHelper.get("ttl", Sample.class, 1, () -> {
            loads.incrementAndGet();
            return new Sample(3L, "after-expire");
        });
        assertEquals(1, loads.get());
        assertEquals("after-expire", value.getName());
    }

    @Test
    @DisplayName("条目数超过上限时自动收敛，不会无界增长")
    void capacityIsBounded() {
        for (int i = 0; i < 5200; i++) {
            cacheHelper.get("cap" + i, Sample.class, 600, () -> new Sample(1L, "v"));
        }

        assertTrue(cacheHelper.cachedKeyCount() <= 5000,
                "缓存条目应被限制在上限内，实际: " + cacheHelper.cachedKeyCount());
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

    /** 空值 TTL 为 60 秒；剩余秒数按整秒截断，允许 59~60 的写法差异 */
    private static void assertShortTtl(long remainingTtlSeconds) {
        assertTrue(remainingTtlSeconds >= 59 && remainingTtlSeconds <= 60,
                "空值 TTL 应为 60 秒，实际: " + remainingTtlSeconds);
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
}
