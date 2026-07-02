package com.example.malllearning.service;

import com.example.malllearning.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 业务队列消费者 —— 演示死信队列
 *
 * 这个消费者监听的 business.queue 配置了死信交换机（DLX）
 * 当处理方法抛出异常时 → 消息进入死信队列 → DeadLetterConsumer 处理
 */
@Component
public class BusinessConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BUSINESS)
    public void handleBusinessMessage(String message) {
        System.out.println("==========================================");
        System.out.println("【业务消费者】收到消息：" + message);
        System.out.println("【业务消费者】开始处理业务...");

        // ★★★ 模拟处理失败：如果消息包含"测试死信"，抛异常
        if (message.contains("【测试死信】")) {
            System.out.println("【❌ 业务消费者】处理失败！抛出异常！");
            System.out.println("【❌ 消息将进入死信队列（因为 business.queue 配置了 DLX）】");
            System.out.println("==========================================");
            throw new RuntimeException("业务处理失败！");
        }

        System.out.println("【业务消费者】处理成功！");
        System.out.println("==========================================");
    }
}
