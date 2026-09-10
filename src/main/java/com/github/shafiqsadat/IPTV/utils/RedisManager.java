package com.github.shafiqsadat.IPTV.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisManager.class);
    private static final int CONNECTION_TIMEOUT_MS = 2000;

    private static volatile JedisPool jedisPool;

    private RedisManager() {
    }

    public static synchronized void init() {
        if (jedisPool != null) {
            return;
        }
        PropertiesReader config = PropertiesReader.getInstance();
        String host = config.getRedisHost();
        int port = config.getRedisPort();

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        JedisPool pool = new JedisPool(poolConfig, host, port, CONNECTION_TIMEOUT_MS, config.getRedisPassword());
        try (Jedis jedis = pool.getResource()) {
            jedis.ping();
        } catch (RuntimeException e) {
            pool.close();
            throw e;
        }
        jedisPool = pool;
        logger.info("Connected to Redis at {}:{}", host, port);
    }

    public static Jedis getJedis() {
        JedisPool pool = jedisPool;
        if (pool == null) {
            throw new IllegalStateException("RedisManager.init() must be called before use");
        }
        return pool.getResource();
    }

    public static synchronized void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection pool closed");
        }
    }
}
