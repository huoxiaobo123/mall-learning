package com.example.malllearning.service;

import com.example.malllearning.config.RabbitMQConfig;
import com.example.malllearning.model.Product;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 消息发送者（Producer）
 *
 * 作用：把消息发送到 RabbitMQ 的 Exchange
 *       Exchange 根据路由键，把消息路由到对应的 Queue
 *
 * 调用方：ProductService（新增商品时调用）
 * 接收方：ProductMessageReceiver（异步处理消息）
 */
@Service
public class ProductMessageSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送商品操作日志消息
     *
     * @param product 被操作的商品
     * @param action  操作类型（"新增"、"更新"、"删除"）
     *
     * RabbitTemplate 是 Spring 提供的操作 RabbitMQ 的工具类
     * 就像 RedisTemplate 操作 Redis，JdbcTemplate 操作 JDBC
     *
     * convertAndSend 的三个参数：
     *   1. exchange    → 发到哪个交换机（product.exchange）
     *   2. routingKey  → 路由键（product.log）
     *   3. message     → 消息内容（这里传商品信息）
     */
    public void sendProductLog(Product product, String action) {
        // 把商品信息和操作类型拼成日志消息
        String message = String.format(
                "【%s】商品 ID=%d, 名称=%s, 价格=%s, 库存=%d",
                action,
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );

        System.out.println("====== 发送消息到 RabbitMQ：" + message + " ======");

        // 把消息发送到 RabbitMQ
        // RabbitTemplate 会自动把 String 序列化后发出去
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,   // 交换机名称
                RabbitMQConfig.ROUTING_KEY,     // 路由键
                message                         // 消息内容
        );

        // 结果：消息到了 RabbitMQ 的 product.exchange
        //       → exchange 看到路由键是 "product.log"
        //       → 找到绑定了 "product.log" 的队列 product.log.queue
        //       → 把消息存到队列里
        //       → 消费者 ProductMessageReceiver 去取
    }
}
