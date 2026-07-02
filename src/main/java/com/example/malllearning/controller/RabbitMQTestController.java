package com.example.malllearning.controller;

import com.example.malllearning.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RabbitMQ 测试控制器
 *
 * 用来验证 RabbitMQ 的生产者 → 交换机 → 队列 → 消费者 流程
 * 不依赖 MySQL 和 Redis，独立测试消息队列
 */
@RestController
@RequestMapping("/mq-test")
public class RabbitMQTestController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送一条消息到 RabbitMQ
     *
     * 测试方法：
     *   GET http://localhost:8081/mq-test/send?msg=你好RabbitMQ
     *
     * 预期结果：
     *   1. 接口立即返回 "消息已发送: xxx"
     *   2. 控制台打印出消费者的接收日志
     *   3. 浏览器打开 http://localhost:15672
     *      → guest/guest 登录
     *      → Queues 选项卡 → 看到 product.log.queue
     *      → 点进去能看到消息详情
     */
    @GetMapping("/send")
    public String sendMessage(@RequestParam(defaultValue = "Hello RabbitMQ!") String msg) {
        System.out.println("====== [生产者] 准备发送消息 ======");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,   // 发到 product.exchange
                RabbitMQConfig.ROUTING_KEY,     // 路由键 product.log
                msg                             // 消息内容
        );

        System.out.println("====== [生产者] 消息已发送: " + msg + " ======");
        return "消息已发送: " + msg;
    }

    /**
     * 模拟下单流程 —— 了解面试常考的"异步下单"场景
     *
     * 测试方法：
     *   GET http://localhost:8081/mq-test/order?userId=100&productId=1&quantity=2
     *
     * 学习重点：
     *   1. 这个方法"立刻返回"，不等待订单处理
     *   2. 消费者在后台异步处理
     *   3. 这就是消息队列的"异步解耦"特性
     */
    @GetMapping("/order")
    public String createOrder(
            @RequestParam(defaultValue = "100") Long userId,
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {

        // 模拟下单消息
        String orderMsg = String.format(
                "【订单】用户 %d 购买了商品 %d，数量 %d",
                userId, productId, quantity
        );

        System.out.println("====== [下单请求] 收到下单请求: " + orderMsg + " ======");
        System.out.println("====== [下单请求] 返回结果给用户，后台异步处理 ======");

        // 发送到 RabbitMQ，异步处理
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                orderMsg
        );

        // 立刻返回！不用等消费者处理完
        return "下单成功！订单正在处理中，请稍后查询结果。";
    }

    /**
     * 秒杀场景模拟 —— 面试高频考点
     *
     * 测试方法：
     *   GET http://localhost:8081/mq-test/seckill?userId=100&productId=1
     *
     * 面试考点：
     *   Q: 秒杀怎么设计？
     *   A: Redis 预减库存 → RabbitMQ 异步处理 → 数据库最终一致性
     */
    @GetMapping("/seckill")
    public String seckill(
            @RequestParam(defaultValue = "100") Long userId,
            @RequestParam(defaultValue = "1") Long productId) {

        String seckillMsg = String.format(
                "【秒杀】用户 %d 抢购商品 %d",
                userId, productId
        );

        System.out.println("====== [秒杀] 用户 " + userId + " 参与秒杀，发送消息 ======");
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                seckillMsg
        );

        return "抢购请求已提交，请等待结果！";
    }
}
