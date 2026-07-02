package com.example.malllearning.service;

import com.example.malllearning.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 死信队列消费者 —— 处理"处理失败"或"超时未支付"的消息
 *
 * ============ 面试必问：订单超时取消 ============
 *
 * 业务流程：
 *   1. 用户下单 → 发一条 TTL=30分钟 的消息到延迟队列
 *   2. 如果 30 分钟内用户支付了 → 从队列删除这条消息
 *   3. 如果 30 分钟没支付 → 消息过期 → 进入死信队列 → 此消费者执行取消订单
 *
 * 当前演示：简化版（30秒超时取消）
 *
 * ============ 面试回答模板 ============
 *
 * Q: 你们项目怎么实现订单超时取消？
 * A: 用 RabbitMQ 死信队列实现。
 *    下单时发送一条 TTL=30min 的延迟消息，
 *    30分钟后消息进入死信队列，
 *    死信消费者从数据库查订单状态，
 *    如果还是"未支付"就执行取消操作。
 *
 * Q: 为什么不用定时任务？
 * A: 定时任务有 1 分钟左右的误差（每分钟扫一次），
 *    而且如果订单量大，每分钟扫描全表性能很差。
 *    死信队列是精确到毫秒级的，且每个订单只触发一次。
 */
@Component
public class DeadLetterConsumer {

    /**
     * 监听死信队列
     *
     * 这个队列收到两种消息：
     *   1. 普通队列中处理失败的消息（业务异常后 nack 过来的）
     *   2. 延迟队列中 TTL 过期的消息（订单超时取消的场景）
     *
     * 参数区别：
     *   String message         → 消息内容（自动反序列化）
     *   Message message        → 消息+元数据（可以拿到消息ID、过期时间等）
     *   Channel channel        → 手动 ACK 时需要
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_DEAD_LETTER)
    public void handleDeadLetter(String message) {
        System.out.println("==========================================");
        System.out.println("【死信队列】收到消息：" + message);
        System.out.println("【死信队列】这条消息之前处理失败了（或超时了）");

        // ===== 订单超时取消场景 =====
        if (message.contains("【订单】") || message.contains("【延迟订单】")) {
            System.out.println("【死信队列】→ 执行【订单超时取消】逻辑");
            System.out.println("【死信队列】→ 查询数据库：订单状态是否为'未支付'？");
            System.out.println("【死信队列】→ 是 → 取消订单，恢复库存");
            System.out.println("【死信队列】→ 通知用户：'您的订单已超时取消'");
        }

        // ===== 普通业务失败场景 =====
        if (message.contains("【库存】")) {
            System.out.println("【死信队列】→ 执行【库存回滚】逻辑");
            System.out.println("【死信队列】→ 库存操作失败，需要人工介入排查");
        }

        System.out.println("==========================================");
    }

    /**
     * 手动 ACK 版本的死信消费者
     *
     * 为什么需要手动 ACK？
     *   默认是自动 ACK：RabbitMQ 把消息发给消费者后，立即标记为"已处理"
     *   如果消费者处理到一半崩溃了，这条消息就丢了（明明没处理完）
     *
     * 手动 ACK：
    *   消费者处理完才告诉 RabbitMQ：'我处理好了'
     *   如果消费者崩溃，消息还在队列里，可以发给其他消费者
     *
     * 🎯 面试考点：
     *   Q: RabbitMQ 怎么保证消息不丢失？
     *   A: 三个方面：
     *      1. 生产者 → 开启 Confirm 模式
     *      2. RabbitMQ 本身 → 队列持久化 + 消息持久化
     *      3. 消费者 → 手动 ACK（处理完才确认）
     *
     * 这里注释掉，用上面的 String 版本更简洁易懂
     */
    // @RabbitListener(queues = RabbitMQConfig.QUEUE_DEAD_LETTER)
    public void handleDeadLetterWithManualAck(Message message, Channel channel) {
        try {
            String msg = new String(message.getBody(), StandardCharsets.UTF_8);
            System.out.println("【手动ACK】处理消息：" + msg);

            // 处理业务逻辑...
            System.out.println("【手动ACK】处理成功！");

            // ★★★ 手动 ACK：告诉 RabbitMQ 我处理完了，可以删消息了
            // 参数1：消息的唯一标识（deliveryTag）
            // 参数2：是否批量确认（false=只确认当前这条）
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            System.out.println("【手动ACK】处理失败：" + e.getMessage());

            try {
                // ★★★ 手动 NACK：告诉 RabbitMQ 我没处理完
                // 参数1：deliveryTag（消息ID）
                // 参数2：是否批量
                // 参数3：requeue（是否重新入队）
                //   true  → 重新放回队列，让消费者再试
                //   false → 不重新入队，直接进死信队列
                channel.basicNack(
                        message.getMessageProperties().getDeliveryTag(),
                        false,
                        false    // false = 不进原队列，进死信队列
                );
            } catch (Exception ex) {
                System.out.println("【手动ACK】NACK 失败：" + ex.getMessage());
            }
        }
    }
}
