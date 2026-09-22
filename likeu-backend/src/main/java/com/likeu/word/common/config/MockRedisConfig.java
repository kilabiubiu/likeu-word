package com.likeu.word.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.Subscription;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发环境模拟 Redis（无需安装 Redis 也能启动）
 *
 * <p>支持过期时间（setEx/pSetEx/expire），与线上 Redis 行为一致，
 * 使 dev 环境也能真实走「token 存 Redis + 校验」的链路。</p>
 */
@Slf4j
@Profile("dev")
@Configuration
public class MockRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.warn("dev 环境启用内存版 Redis 模拟，重启后数据丢失；如需真实 Redis 请用 local/prod profile");
        return new MockRedisConnectionFactory();
    }

    private static class MockRedisConnectionFactory implements RedisConnectionFactory {

        private final MockRedisConnection connection = new MockRedisConnection();

        @Override
        public RedisConnection getConnection() {
            return connection;
        }

        @Override
        public RedisClusterConnection getClusterConnection() {
            return null;
        }

        @Override
        public boolean getConvertPipelineAndTxResults() {
            return false;
        }

        @Override
        public RedisSentinelConnection getSentinelConnection() {
            return null;
        }

        @Override
        public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
            return null;
        }
    }

    /**
     * 使用 DefaultedRedisConnection 接口，提供所有命令方法的默认实现
     * 只需要覆盖 get/set/del/exists/expire/ttl 等关键方法
     */
    private static class MockRedisConnection implements DefaultedRedisConnection {

        /** 一条缓存记录：值 + 过期时间戳（0 表示永不过期） */
        private static class Entry {
            final byte[] value;
            final long expireAtMillis;

            Entry(byte[] value, long expireAtMillis) {
                this.value = value;
                this.expireAtMillis = expireAtMillis;
            }

            boolean expired() {
                return expireAtMillis > 0 && System.currentTimeMillis() > expireAtMillis;
            }
        }

        /** key 用 String 保存：byte[] 没有内容级 equals/hashCode，直接当 key 会导致永远读不中 */
        private final Map<String, Entry> store = new ConcurrentHashMap<>();

        private static String keyOf(byte[] key) {
            return new String(key, StandardCharsets.UTF_8);
        }

        private Entry liveEntry(byte[] key) {
            Entry entry = store.get(keyOf(key));
            if (entry == null) {
                return null;
            }
            if (entry.expired()) {
                store.remove(keyOf(key));
                return null;
            }
            return entry;
        }

        @Override
        public byte[] get(byte[] key) {
            Entry entry = liveEntry(key);
            return entry == null ? null : entry.value;
        }

        @Override
        public Boolean set(byte[] key, byte[] value) {
            store.put(keyOf(key), new Entry(value, 0));
            return true;
        }

        @Override
        public Boolean setNX(byte[] key, byte[] value) {
            if (liveEntry(key) != null) {
                return false;
            }
            store.put(keyOf(key), new Entry(value, 0));
            return true;
        }

        @Override
        public Boolean setEx(byte[] key, long seconds, byte[] value) {
            store.put(keyOf(key), new Entry(value, System.currentTimeMillis() + seconds * 1000));
            return true;
        }

        @Override
        public Boolean pSetEx(byte[] key, long milliseconds, byte[] value) {
            store.put(keyOf(key), new Entry(value, System.currentTimeMillis() + milliseconds));
            return true;
        }

        @Override
        public Boolean exists(byte[] key) {
            return liveEntry(key) != null;
        }

        @Override
        public Long del(byte[]... keys) {
            long count = 0;
            for (byte[] key : keys) {
                if (store.remove(keyOf(key)) != null) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Boolean expire(byte[] key, long seconds) {
            Entry entry = liveEntry(key);
            if (entry == null) {
                return false;
            }
            store.put(keyOf(key), new Entry(entry.value, System.currentTimeMillis() + seconds * 1000));
            return true;
        }

        @Override
        public Boolean pExpire(byte[] key, long milliseconds) {
            Entry entry = liveEntry(key);
            if (entry == null) {
                return false;
            }
            store.put(keyOf(key), new Entry(entry.value, System.currentTimeMillis() + milliseconds));
            return true;
        }

        @Override
        public Long ttl(byte[] key) {
            Entry entry = liveEntry(key);
            if (entry == null) {
                return -2L;
            }
            if (entry.expireAtMillis == 0) {
                return -1L;
            }
            return Math.max(0, (entry.expireAtMillis - System.currentTimeMillis()) / 1000);
        }

        @Override
        public Long pTtl(byte[] key) {
            Long ttl = ttl(key);
            return ttl == null || ttl < 0 ? ttl : ttl * 1000;
        }

        @Override
        public Long dbSize() {
            return (long) store.size();
        }

        @Override
        public void flushDb() {
            store.clear();
        }

        @Override
        public String ping() {
            return "PONG";
        }

        @Override
        public Object execute(String commandName, byte[]... args) {
            return null;
        }

        @Override
        public void multi() {
        }

        @Override
        public void discard() {
        }

        @Override
        public List<Object> exec() {
            return Collections.emptyList();
        }

        @Override
        public void watch(byte[]... keys) {
        }

        @Override
        public void unwatch() {
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }

        @Override
        public Subscription getSubscription() {
            return null;
        }

        @Override
        public Long publish(byte[] channel, byte[] message) {
            return 0L;
        }

        @Override
        public void subscribe(MessageListener listener, byte[]... channels) {
        }

        @Override
        public void pSubscribe(MessageListener listener, byte[]... patterns) {
        }

        @Override
        public void select(int dbIndex) {
        }

        @Override
        public byte[] echo(byte[] message) {
            return message;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public Object getNativeConnection() {
            return null;
        }

        @Override
        public boolean isQueueing() {
            return false;
        }

        @Override
        public boolean isPipelined() {
            return false;
        }

        @Override
        public void openPipeline() {
        }

        @Override
        public List<Object> closePipeline() {
            return Collections.emptyList();
        }

        @Override
        public RedisSentinelConnection getSentinelConnection() {
            return null;
        }
    }
}
