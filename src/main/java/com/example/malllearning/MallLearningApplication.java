package com.example.malllearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;

// 临时排除 Redis（MySQL 和 RabbitMQ 正常运行）
@SpringBootApplication(exclude = {
        DataRedisAutoConfiguration.class
})
public class MallLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallLearningApplication.class, args);
    }

}
