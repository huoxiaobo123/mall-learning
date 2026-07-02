package com.example.malllearning.service;

import com.example.malllearning.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Topic 通配符模式消费者
 *
 * 演示 Topic 交换机的"通配符匹配"
 *
 * 路由规则：
 *   log.error → error 队列 ✅  all 队列 ✅
 *   log.warn  →                   all 队列 ✅
 *   log.info  →                   all 队列 ✅
 *   product.log →                 all 队列 ✅（因为 log.# 匹配所有以 log. 开头的）
 */
@Component
public class TopicConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TOPIC_ERROR)
    public void handleErrorLog(String message) {
        System.out.println("【🚨 错误日志】" + message);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TOPIC_ALL)
    public void handleAllLog(String message) {
        System.out.println("【📋 全部日志】" + message);
    }
}
