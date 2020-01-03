package com.atguigu.gmall.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author GPX
 * @date 2019/12/30 19:47
 */
public class RedisUtil {
    /*
     0. 创建JedisPool
     1. 获取Jedis
      */
    // 创建JedisPool
    private JedisPool jedisPool;

    // String host = "192.168.126.129";
    // 初始化连接池
    public void initJedisPool(String host, int port, int timeOut, int database) {
        // 创建配置连接池的参数类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 设置连接池最大核心数
        jedisPoolConfig.setMaxTotal(200);
        // 设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);
        // 最少剩余数
        jedisPoolConfig.setMinIdle(10);
        // 排队等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 设置当用户获取到jedis 时，做自检看当前获取到的jedis 是否可以使用！
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig, host, port, timeOut);
    }

    // 获取Jedis
    public Jedis getJedis() {
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }
}
