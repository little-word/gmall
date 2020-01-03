package com.atguigu.gmall.config;

import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author GPX
 * @date 2019/12/30 19:46
 */
@Configuration
public class RedisConfig {
    // :disabled 表示如果配置文件中没有获取到host ，则表示默认值disabled
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeOut:10000}")
    private int timeOut;

    /*
    <bean id = "redisUtil" class="com.atguigu.gmall0311.config.RedisUtil">
    </bean>
     */

    @Bean
    public RedisUtil getRedisUtil() {
        // 表示配置文件中没有host
        if ("disabled".equals(host)) {
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host, port, timeOut, database);
        return redisUtil;
    }
}
