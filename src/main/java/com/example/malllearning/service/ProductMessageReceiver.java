package com.example.malllearning.service;

import com.example.malllearning.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消息接收者（Consumer）
 *
 * ★ 增强版：包含死信模拟 + 手动 ACK 示意图
 */
@Component
public class ProductMessageReceiver {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(String message) {
        System.out.println("========================================");
        System.out.println("【RabbitMQ 消费者】收到消息：" + message);
        System.out.println("【处理时间】" + new java.util.Date());
        System.out.println("========================================");

        // ★★★ 死信队列演示：如果消息包含"测试死信"，模拟处理失败
        if (message.contains("【测试死信】")) {
            System.out.println("========================================");
            System.out.println("【❌ 模拟处理失败】这条消息将进入死信队列！");
            System.out.println("【❌ 原因】消费者主动抛出异常，RabbitMQ 重试3次后送入死信队列");
            System.out.println("【❌ 查看死信队列】http://localhost:15672 → Queues → dead.letter.queue");
            System.out.println("========================================");
            // 抛异常 → RabbitMQ 重试（默认3次）→ 3次都失败 → 进死信队列
            // 注意：默认配置下，重试3次后消息会一直留在队列里（不是自动进死信队列）
            // 需要配置 spring.rabbitmq.listener.simple.retry.enabled=true
            // 并且配置死信队列才能自动转入
            throw new RuntimeException("模拟处理失败，消息将进入死信队列！");
        }

        // ★★★ 幂等性演示：如果消息包含"幂等测试"，模拟已被处理过
        if (message.contains("【幂等测试】")) {
            System.out.println("========================================");
            System.out.println("【🔁 幂等性检查】收到消息，检查是否已处理...");
            System.out.println("【🔁 幂等性检查】查询去重表：messageId 是否已存在？");
            System.out.println("【🔁 幂等性检查】→ 已存在，直接 ACK 丢弃（不重复处理）");
            System.out.println("========================================");
            return; // 直接返回，不抛异常
        }

        // 正常处理
        System.out.println("【RabbitMQ 消费者】消息处理完成！");
    }
}
