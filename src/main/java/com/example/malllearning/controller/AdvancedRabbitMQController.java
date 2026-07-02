package com.example.malllearning.controller;

import com.example.malllearning.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * RabbitMQ 高级功能测试控制器
 *
 * 覆盖面试考点：
 *   1. 死信队列 —— 消息处理失败后去哪了
 *   2. 延迟队列 —— 订单超时取消（最高频面试题）
 *   3. Fanout 广播 —— 消息发给所有服务
 *   4. Topic 通配符 —— 日志分级
 *   5. 幂等性 —— 防止重复消费
 */
@RestController
@RequestMapping("/mq-advanced")
public class AdvancedRabbitMQController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // =============================================
    //   考点 1：死信队列 —— 模拟消息处理失败
    //
    //   发送一条消息到普通队列，但设置它"一定会失败"
    //   失败后消息进入死信队列，由 DeadLetterConsumer 处理
    //
    //   测试：GET http://localhost:8081/mq-advanced/dead-letter
    //   预期：
    //     1. 消息先到 product.log.queue
    //     2. 消费者处理失败 → 进入 dead.letter.queue
    //     3. 死信消费者打印"这条消息之前处理失败了"
    // =============================================
    @GetMapping("/dead-letter")
    public String testDeadLetter(@RequestParam(defaultValue = "测试消息-处理失败") String msg) {
        // 发到带死信配置的业务队列
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.ROUTING_BUSINESS,  // 发到 business.queue（有 DLX）
                "【测试死信】" + msg
        );
        return "消息已发送到 business.queue，将模拟处理失败并进入死信队列！";
    }

    // =============================================
    //   考点 2：延迟队列 / 订单超时取消（面试高频）
    //
    //   原理：消息发到 order.delay.queue，设置 TTL=30秒
    //        30秒后消息过期 → 进入死信队列 → 执行"取消订单"
    //
    //   测试：GET http://localhost:8081/mq-advanced/delay-order?userId=1
    //   预期：
    //     1. 立即返回 "下单成功，30秒未支付将自动取消"
    //     2. 30秒后控制台打印死信消费者处理"取消订单"
    //     3. 如果期间（30秒内）调用了 pay?orderId=xxx，则取消不会执行
    //
    //   🎯 面试回答：
    //     Q: 怎么实现订单超时取消？
    //     A: 利用 RabbitMQ 死信队列 + TTL。
    //        下单时发送一条 TTL=30min 的消息到延迟队列，
    //        过期后进入死信队列，死信消费者执行取消。
    //        如果用户支付了，把延迟消息移除。
    // =============================================
    @GetMapping("/delay-order")
    public String delayOrder(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "100") Long orderId,
            @RequestParam(defaultValue = "30") Integer ttlSeconds) {

        String orderMsg = String.format(
                "【延迟订单】订单ID=%d, 用户ID=%d, 金额=￥1999, 下单时间=%d秒前",
                orderId, userId, ttlSeconds
        );

        System.out.println("===== 【下单】用户 " + userId + " 提交订单 " + orderId + " =====");
        System.out.println("===== 【下单】TTL=" + ttlSeconds + "秒后未支付将自动取消 =====");

        // ★★★ 核心：发送延迟消息，设置 TTL（消息过期时间）
        //   TTL 过期后 → 消息变死信 → 进死信队列 → DeadLetterConsumer 处理
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_DELAY,
                RabbitMQConfig.ROUTING_DELAY,
                orderMsg,
                message -> {
                    // 设置这条消息的过期时间（毫秒）
                    message.getMessageProperties().setExpiration(String.valueOf(ttlSeconds * 1000));
                    return message;
                }
        );

        return "下单成功！订单 " + orderId + " 将在 " + ttlSeconds + " 秒后自动取消（如果未支付）。" +
                "你可以在这期间访问 /mq-advanced/pay?orderId=" + orderId + " 模拟支付。";
    }

    /**
     * 模拟支付 —— 如果支付了，把延迟消息干掉，不让取消
     *
     * 测试：GET http://localhost:8081/mq-advanced/pay?orderId=100
     */
    @GetMapping("/pay")
    public String pay(@RequestParam Long orderId) {
        // ⚠️ 实际项目中这里应该：
        //   1. 更新数据库订单状态为"已支付"
        //   2. 从延迟队列删除对应的消息
        //   3. 发送"支付成功"通知消息
        System.out.println("===== 【支付】用户已支付订单 " + orderId + " =====");
        System.out.println("===== 【支付】移除延迟队列中的消息（防止被取消） =====");
        return "支付成功！订单 " + orderId + " 已支付，不会被执行取消操作。";
    }

    // =============================================
    //   考点 3：Fanout 广播
    //
    //   测试：GET http://localhost:8081/mq-advanced/broadcast
    //   预期：broadcast.queue.a 和 broadcast.queue.b 都收到消息
    //
    //   🎯 面试考点：
    //     Q: 什么场景用 Fanout 交换机？
    //     A: 需要广播给所有服务时，比如配置更新、系统公告。
    //        库存服务、搜索服务、推荐服务都要收到"新商品上架"通知。
    // =============================================
    @GetMapping("/broadcast")
    public String testFanout(@RequestParam(defaultValue = "新商品上架啦！") String msg) {
        System.out.println("===== 【广播】发送消息到 Fanout 交换机 =====");
        System.out.println("===== 【广播】所有绑定的队列都会收到 =====");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_FANOUT,
                "",    // Fanout 不需要路由键
                "【Fanout广播】" + msg
        );

        return "广播消息已发送，所有队列都已收到！";
    }

    // =============================================
    //   考点 4：Topic 通配符模式
    //
    //   测试：
    //     GET http://localhost:8081/mq-advanced/topic?key=log.error&msg=数据库连接失败
    //     GET http://localhost:8081/mq-advanced/topic?key=log.warn&msg=磁盘空间不足
    //     GET http://localhost:8081/mq-advanced/topic?key=log.info&msg=用户登录成功
    //
    //   预期：
    //     log.error → topic.log.error ✅（精确匹配）+ topic.log.all ✅（通配匹配）
    //     log.warn  → topic.log.all ✅（通配匹配，但 error 队列不收）
    //     log.info  → topic.log.all ✅
    //
    //   🎯 面试考点：
    //     Q: Topic 的 # 和 * 区别？
    //     A: * 匹配一个词（order.*），# 匹配零个或多个词（order.#）
    //        ex: order.*   → order.create ✅  order.pay.paid ❌
    //            order.#   → order.create ✅  order.pay.paid ✅
    // =============================================
    @GetMapping("/topic")
    public String testTopic(
            @RequestParam(defaultValue = "log.error") String key,
            @RequestParam(defaultValue = "数据库连接失败") String msg) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_TOPIC,
                key,
                "【Topic】" + msg
        );

        return String.format(
                "Topic 消息已发送！路由键=%s", key
        );
    }

    // =============================================
    //   考点 5：幂等性 —— 防止重复消费
    //
    //   📖 费曼解释：
    //     幂等性 = 同一个操作执行多次和执行一次结果一样
    //     就像"把灯关掉"（灯已经关了，再按一次还是关着的）
    //     而不是"按一下开/关切换"（按两次就不一样了）
    //
    //   场景：RabbitMQ 可能因为网络原因，把同一条消息发两次
    //        如果不做幂等性，用户会被扣两次积分、发两次短信
    //
    //   解决方案：
    //     1. 每条消息带唯一 ID（messageId）
    //     2. 消费者处理前先查去重表
    //     3. 已经处理过 → 丢弃
    //
    //   测试：GET http://localhost:8081/mq-advanced/idempotent
    //   预期：发送一条带唯一ID的消息，多次测试相同的消息会被去重
    //
    //   🎯 面试高频：
    //     Q: 怎么解决重复消费？
    //     A: 业务层面做幂等性。
    //        每条消息携带全局唯一 ID（UUID），消费者处理前先查
    //        数据库去重表，已处理过的直接 ACK 丢弃。
    //        核心思想：接口设计成天然幂等的。
    // =============================================
    @GetMapping("/idempotent")
    public String testIdempotent() {
        // 生成全局唯一消息ID
        String messageId = UUID.randomUUID().toString();

        String msg = "【幂等测试】消息ID=" + messageId + ", 内容=给用户加100积分";

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_DIRECT,
                RabbitMQConfig.ROUTING_DIRECT,
                msg,
                message -> {
                    // 在消息头中设置唯一ID（消费者可以用这个去重）
                    message.getMessageProperties().setMessageId(messageId);
                    // 模拟重复发送（同一内容发两次）
                    System.out.println("===== 【幂等】消息ID=" + messageId + "，这条消息可能会被多次投递 =====");
                    return message;
                }
        );

        return "带唯一ID的消息已发送！消息ID=" + messageId;
    }
}
