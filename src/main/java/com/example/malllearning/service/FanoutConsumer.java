package com.example.malllearning.service;

import com.example.malllearning.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Fanout 广播模式消费者
 *
 * 演示：一条消息被多个队列收到
 * 场景：新商品上架 → 库存服务 + 搜索服务 + 推荐服务都要知道
 */
@Component
public class FanoutConsumer {

    /**
     * 服务A —— 库存服务
     * 收到广播后：更新库存缓存
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_FANOUT_A)
    public void handleBroadcastA(String message) {
        System.out.println("【📦 库存服务】收到广播：" + message);
        System.out.println("【📦 库存服务】→ 更新库存缓存");
    }

    /**
     * 服务B —— 搜索服务
     * 收到广播后：更新搜索引擎索引
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_FANOUT_B)
    public void handleBroadcastB(String message) {
        System.out.println("【🔍 搜索服务】收到广播：" + message);
        System.out.println("【🔍 搜索服务】→ 更新 ES 索引");
    }
}
