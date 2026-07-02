package com.example.malllearning.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 完整配置 —— 覆盖所有面试考点
 *
 * ===================== 本配置包含 =====================
 *
 *   Part 1: 基本 Direct 模式（已有）
 *   Part 2: 死信队列 DLQ（面试必考）
 *   Part 3: 延迟队列 / 订单超时取消（高频面试题）
 *   Part 4: Fanout 广播模式
 *   Part 5: Topic 通配符模式
 *
 * ===================== 概念关系图 =====================
 *
 *   ┌─────────────────────────────────────────────┐
 *   │                  生产者                       │
 *   │  (Publisher Confirm + Return Callback 保证)   │
 *   └──────────┬──────────────────────────────────┘
 *              │ 发消息
 *   ┌──────────▼──────────────────────────────────┐
 *   │            交换机 (Exchange)                  │
 *   │  Direct: 完全匹配  │  Fanout: 广播            │
 *   │  Topic: 通配符匹配  │                           │
 *   └──────────┬──────────────────────────────────┘
 *              │ 路由
 *   ┌──────────▼──────────────────────────────────┐
 *   │              队列 (Queue)                     │
 *   │  durable=true（持久化）                        │
 *   │  TTL 过期 → 进入死信队列                       │
 *   └──────────┬──────────────────────────────────┘
 *              │ 消费
 *   ┌──────────▼──────────────────────────────────┐
 *   │              消费者                           │
 *   │  手动 ACK（处理完才确认）                      │
 *   │  幂等性处理（防止重复消费）                     │
 *   │  失败 → Nack → 死信队列                       │
 *   └─────────────────────────────────────────────┘
 */
@Configuration
public class RabbitMQConfig {

    // =============================================
    //          Part 1: 已有 Direct 模式
    //          （保留旧常量名兼容老代码）
    // =============================================

    // 新名称
    public static final String EXCHANGE_DIRECT = "product.exchange";
    public static final String QUEUE_DIRECT = "product.log.queue";
    public static final String ROUTING_DIRECT = "product.log";

    // ★ 旧名称（兼容 ProductMessageSender/ProductMessageReceiver）
    public static final String EXCHANGE_NAME = EXCHANGE_DIRECT;
    public static final String QUEUE_NAME = QUEUE_DIRECT;
    public static final String ROUTING_KEY = ROUTING_DIRECT;

    @Bean
    public Queue directQueue() {
        // durable=true → RabbitMQ 重启后队列还在
        return QueueBuilder.durable(QUEUE_DIRECT).build();
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT, true, false);
    }

    @Bean
    public Binding directBinding() {
        return BindingBuilder
                .bind(directQueue())
                .to(directExchange())
                .with(ROUTING_DIRECT);
    }

    // =============================================
    //   Part 1b: 业务队列（带死信配置）
    //   和 Part 1 用同一个交换机，但路由键不同
    //
    //   这个队列配置了 DLX，消息处理失败会自动转入死信队列
    //   用于演示"消息处理失败 → 死信队列 → 人工排查"
    // =============================================

    public static final String QUEUE_BUSINESS = "business.queue";
    public static final String ROUTING_BUSINESS = "business";

    @Bean
    public Queue businessQueue() {
        Map<String, Object> args = new HashMap<>();
        // ★ 配置死信交换机
        args.put("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER);
        args.put("x-dead-letter-routing-key", ROUTING_DEAD_LETTER);
        return QueueBuilder.durable(QUEUE_BUSINESS)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding businessBinding() {
        return BindingBuilder
                .bind(businessQueue())
                .to(directExchange())  // 绑定到同一个 Direct Exchange
                .with(ROUTING_BUSINESS);
    }

    // =============================================
    //   Part 2: 死信队列（Dead Letter Queue）
    //
    //   📖 费曼解释：
    //      死信队列 = 医院急救室
    //      消息处理失败（抛异常）→ 判为"死信"→ 送进死信队列
    //      开发人员去死信队列查看失败原因
    //
    //   ❓ 什么时候消息变成死信？
    //      1. 消费者拒绝确认（basicNack / basicReject）且不重回队列
    //      2. 消息 TTL 过期（下面 Part 3 演示）
    //      3. 队列达到最大长度
    //
    //   🎯 面试考点：
    //      Q: 消息处理失败了怎么办？
    //      A: 配置死信队列，失败的消息自动转入死信队列，
    //         方便排查问题，也可以重新入队重试
    // =============================================

    /** 死信队列名称 */
    public static final String QUEUE_DEAD_LETTER = "dead.letter.queue";
    /** 死信交换机名称 */
    public static final String EXCHANGE_DEAD_LETTER = "dead.letter.exchange";
    /** 死信路由键 */
    public static final String ROUTING_DEAD_LETTER = "dead.letter";

    /**
     * 死信队列
     * 专门存放"处理失败"的消息
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(QUEUE_DEAD_LETTER, true);
    }

    /**
     * 死信交换机（Direct 类型）
     * 当消息变成死信时，会被路由到这个交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DEAD_LETTER, true, false);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(ROUTING_DEAD_LETTER);
    }

    // =============================================
    //   Part 3: 延迟队列（订单超时取消）
    //
    //   📖 费曼解释：
    //      延迟队列 = 给消息设一个"定时炸弹"
    //      消息进入队列后开始倒计时（TTL = Time To Live）
    //      倒计时结束还没被消费 → 消息爆炸（变成死信）→ 进入死信队列
    //      死信队列的消费者收到消息后，执行"取消订单"
    //
    //   实际场景：
    //      用户下单 → 发一条延迟消息（TTL=30分钟）
    //      如果 30 分钟内用户支付了 → 把这条消息删除
    //      如果 30 分钟没支付 → 消息过期 → 进入死信队列 → 取消订单
    //
    //   🎯 面试高频：
    //      Q: RabbitMQ 怎么实现延迟消息？
    //      A: 利用死信队列 + TTL。消息设置过期时间，
    //         过期后变死信进入死信队列，消费者消费死信执行对应操作
    //
    //      Q: 项目中订单超时取消怎么实现的？
    //      A: 用户下单后发一条 TTL=30min 的消息到延迟队列，
    //         30min 后消息过期进入死信队列，死信消费者执行取消订单。
    //         如果用户中途支付了，把延迟消息移除。
    // =============================================

    /** 延迟队列（业务队列，消息在这里等过期） */
    public static final String QUEUE_DELAY = "order.delay.queue";
    /** 延迟交换机 */
    public static final String EXCHANGE_DELAY = "order.delay.exchange";
    /** 延迟路由键 */
    public static final String ROUTING_DELAY = "order.delay";

    /**
     * 延迟队列
     *
     * 关键配置：x-dead-letter-exchange + x-dead-letter-routing-key
     * 这两行告诉 RabbitMQ：这个队列里的消息过期后，发到哪个死信交换机
     *
     * 流程：
     *   order.delay.queue（消息等 30 分钟）
     *       ↓ 过期
     *   dead.letter.exchange（死信交换机）
     *       ↓
     *   dead.letter.queue（死信队列，真正的"取消订单"在这里处理）
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        // ★★★ 核心：指定死信交换机（消息过期后去这里）
        args.put("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER);
        // ★★★ 核心：指定死信路由键（死信交换机用这个键找队列）
        args.put("x-dead-letter-routing-key", ROUTING_DEAD_LETTER);
        // 可选：队列中消息的最大存活时间（ms），也可以在发消息时单独设置
        // args.put("x-message-ttl", 60000); // 1分钟

        return QueueBuilder.durable(QUEUE_DELAY)
                .withArguments(args)
                .build();
    }

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(EXCHANGE_DELAY, true, false);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder
                .bind(delayQueue())
                .to(delayExchange())
                .with(ROUTING_DELAY);
    }

    // =============================================
    //   Part 4: Fanout 广播模式
    //
    //   📖 费曼解释：
    //      Fanout = 大喇叭广播
    //      不管路由键是什么，消息发给所有绑定了这个交换机的队列
    //
    //   场景：新商品上架 → 通知所有服务（库存服务、搜索服务、推荐服务）
    //
    //   🎯 面试考点：
    //      Q: 三种 Exchange 有什么区别？
    //      A: Direct 精确匹配，Topic 通配符匹配，Fanout 广播给所有人
    // =============================================

    public static final String EXCHANGE_FANOUT = "broadcast.exchange";
    public static final String QUEUE_FANOUT_A = "broadcast.queue.a";  // 库存服务
    public static final String QUEUE_FANOUT_B = "broadcast.queue.b";  // 搜索服务

    @Bean
    public Queue fanoutQueueA() {
        return new Queue(QUEUE_FANOUT_A, true);
    }

    @Bean
    public Queue fanoutQueueB() {
        return new Queue(QUEUE_FANOUT_B, true);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_FANOUT, true, false);
    }

    @Bean
    public Binding fanoutBindingA() {
        return BindingBuilder
                .bind(fanoutQueueA())
                .to(fanoutExchange());
    }

    @Bean
    public Binding fanoutBindingB() {
        return BindingBuilder
                .bind(fanoutQueueB())
                .to(fanoutExchange());
    }

    // =============================================
    //   Part 5: Topic 通配符模式
    //
    //   📖 费曼解释：
    //      Topic = 按规则匹配，类似"按片区送快递"
    //      * 匹配一个词（如：order.* → order.create ✅ order.pay.paid ❌）
    //      # 匹配零个或多个词（如：order.# → order.create ✅ order.pay.paid ✅）
    //
    //   场景：日志系统
    //      log.error → error 队列
    //      log.warn  → warn 队列
    //      log.info  → info 队列
    //      log.#     → 所有日志
    //
    //   🎯 面试考点：
    //      Q: Topic 和 Direct 区别？
    //      A: Direct 完全匹配，Topic 通配符匹配，更灵活
    // =============================================

    public static final String EXCHANGE_TOPIC = "topic.log.exchange";
    public static final String QUEUE_TOPIC_ERROR = "topic.log.error";
    public static final String QUEUE_TOPIC_ALL = "topic.log.all";
    public static final String ROUTING_ERROR = "log.error";
    public static final String ROUTING_WARN = "log.warn";
    public static final String ROUTING_ALL = "log.#";  // # 匹配所有

    @Bean
    public Queue topicErrorQueue() {
        return new Queue(QUEUE_TOPIC_ERROR, true);
    }

    @Bean
    public Queue topicAllQueue() {
        return new Queue(QUEUE_TOPIC_ALL, true);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_TOPIC, true, false);
    }

    @Bean
    public Binding topicErrorBinding() {
        return BindingBuilder
                .bind(topicErrorQueue())
                .to(topicExchange())
                .with("log.error");  // 只收 error 级别的日志
    }

    @Bean
    public Binding topicAllBinding() {
        return BindingBuilder
                .bind(topicAllQueue())
                .to(topicExchange())
                .with("log.#");     // 收所有日志
    }
}
