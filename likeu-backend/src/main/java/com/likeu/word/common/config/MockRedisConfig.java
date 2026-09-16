package com.likeu.word.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发环境模拟 Redis（无需安装 Redis 也能启动）
 * 兼容 Spring Data Redis 2.1.5
 */
@Profile("dev")
@Configuration
public class MockRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new MockRedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
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
     * 只需要覆盖需要的 get/set/del/exists 方法即可
     */
    private static class MockRedisConnection implements DefaultedRedisConnection {

        private final Map<byte[], byte[]> store = new ConcurrentHashMap<>();

        @Override
        public byte[] get(byte[] key) {
            return store.get(key);
        }

        @Override
        public Boolean set(byte[] key, byte[] value) {
            store.put(key, value);
            return true;
        }

        @Override
        public Boolean setNX(byte[] key, byte[] value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setEx(byte[] key, long seconds, byte[] value) {
            store.put(key, value);
            return true;
        }

        @Override
        public Boolean pSetEx(byte[] key, long milliseconds, byte[] value) {
            store.put(key, value);
            return true;
        }

        @Override
        public Boolean exists(byte[] key) {
            return store.containsKey(key);
        }

        @Override
        public Long del(byte[]... keys) {
            long count = 0;
            for (byte[] key : keys) {
                if (store.remove(key) != null) count++;
            }
            return count;
        }

        @Override
        public Boolean expire(byte[] key, long seconds) {
            return store.containsKey(key);
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