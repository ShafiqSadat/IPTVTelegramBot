package com.github.shafiqsadat.IPTV.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisManager.class);
    private static JedisPool jedisPool;

    static {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            // Create JedisPool with hostname and port explicitly
            jedisPool = new JedisPool(poolConfig, "localhost", 6379);
            
            // Test the connection
            try (Jedis testJedis = jedisPool.getResource()) {
                testJedis.ping();
            }
            
            logger.info("Redis connection pool initialized successfully");
            System.out.println("✅ Redis connection pool initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Redis connection pool", e);
            System.err.println("❌ Failed to initialize Redis connection pool: " + e.getMessage());
            System.err.println("💡 Make sure Redis is running: brew services start redis");
            e.printStackTrace();
        }
    }

    public static Jedis getJedis() {
        if (jedisPool == null) {
            System.err.println("❌ Redis pool is not initialized!");
            throw new RuntimeException("Redis pool is not initialized. Make sure Redis is running on localhost:6379");
        }
        try {
            Jedis jedis = jedisPool.getResource();
            // Test connection
            jedis.ping();
            return jedis;
        } catch (Exception e) {
            System.err.println("❌ Failed to get Redis connection: " + e.getMessage());
            throw new RuntimeException("Failed to connect to Redis. Make sure Redis is running on localhost:6379", e);
        }
    }

    public static void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection pool closed");
        }
    }
}
