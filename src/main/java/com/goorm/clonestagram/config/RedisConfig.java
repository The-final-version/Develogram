package com.goorm.clonestagram.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    // 비밀번호가 있는 경우 주석 해제 및 설정
    // @Value("${spring.data.redis.password}")
    // private String redisPassword;

    @Bean(destroyMethod = "shutdown") // 앱 종료 시 Redisson client 종료
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer()
              .setAddress(redisAddress);
              // .setPassword(redisPassword); // 비밀번호 설정

        return Redisson.create(config);
    }
} 