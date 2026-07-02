# Mall-Learning 🛒

基于 **Spring Boot + MyBatis-Plus + Redis + RabbitMQ** 的电商后端学习项目。

> 通过 mall 商城项目学习 Java 后端核心技术，代码带有详细的**费曼学习法注释**，适合面试复习。

---

## 📋 技术栈

| 技术 | 用途 | 状态 |
|------|------|------|
| Spring Boot 4.0 | Web 框架 | ✅ |
| MyBatis-Plus | ORM / 数据库操作 | ✅ |
| MySQL 8.0 | 数据库 | ✅ |
| Redis | 缓存加速 | ⏳ 待集成 |
| RabbitMQ | 消息队列 / 异步解耦 | ✅ |
| Docker | 容器化部署 | ⏳ 待学习 |

---

## 🚀 快速启动

### 前置条件
- JDK 17+
- MySQL 8.0（端口 3306，root/1234）
- RabbitMQ（端口 5672，guest/guest）
- Maven 3.9+

### 启动步骤

```bash
# 1. 创建数据库
mysql -uroot -p1234 -e "CREATE DATABASE IF NOT EXISTS mall_learning DEFAULT CHARSET utf8mb4;"

# 2. 启动项目
mvn spring-boot:run
```

### 测试接口

```bash
# 商品列表
curl http://localhost:8081/product/list

# RabbitMQ 消息发送
curl "http://localhost:8081/mq-test/send?msg=HelloMQ"

# RabbitMQ 管理界面
# http://localhost:15672  (guest/guest)
```

---

## 📚 核心功能

### 商品管理（CRUD + 缓存）
- 商品增删改查
- MyBatis-Plus 动态条件查询
- Redis 缓存加速（待启动 Redis 后生效）

### RabbitMQ 消息队列（🌟 面试重点）

| 功能 | 对应文件 | 面试考点 |
|------|---------|---------|
| Direct 直连模式 | `RabbitMQConfig.java` | 基础消息路由 |
| **死信队列 DLQ** | `DeadLetterConsumer.java` | 消息处理失败机制 |
| **延迟队列/订单超时取消** | `AdvancedRabbitMQController.java` | 🔥 最高频面试题 |
| **Fanout 广播模式** | `FanoutConsumer.java` | 三种交换机区别 |
| **Topic 通配符模式** | `TopicConsumer.java` | `#` vs `*` 区别 |
| **幂等性处理** | `ProductMessageReceiver.java` | 防止重复消费 |
| **手动 ACK** | `DeadLetterConsumer.java` | 消息不丢失保证 |

---

## 🎯 面试话术

> "我独立搭建了这个电商后端项目，基于 Spring Boot + MyBatis-Plus + Redis + RabbitMQ。
> 核心是商品管理和订单流程。我重点优化了缓存和消息队列部分：
> - 使用 Redis 缓存热点数据，设置随机过期时间防止缓存雪崩
> - 基于 RabbitMQ 死信队列实现订单超时取消功能
> - 使用消息确认机制保证消息不丢失
> - 接口设计具有幂等性，防止重复消费"

---

## 📁 项目结构

```
src/main/java/com/example/malllearning/
├── MallLearningApplication.java    # 启动类
├── config/
│   ├── RabbitMQConfig.java         # RabbitMQ 完整配置（5种模式）
│   └── RedisConfig.java            # Redis 配置
├── controller/
│   ├── ProductController.java      # 商品 CRUD 接口
│   ├── RabbitMQTestController.java # MQ 基础测试
│   └── AdvancedRabbitMQController.java  # MQ 高级功能测试
├── mapper/
│   └── ProductMapper.java          # MyBatis-Plus Mapper
├── model/
│   └── Product.java                # 商品实体
└── service/
    ├── ProductService.java         # 商品业务逻辑（含缓存 + MQ）
    ├── ProductMessageSender.java   # RabbitMQ 生产者
    ├── ProductMessageReceiver.java # RabbitMQ 消费者
    ├── BusinessConsumer.java       # 业务队列消费者（死信演示）
    ├── DeadLetterConsumer.java     # 死信队列消费者
    ├── FanoutConsumer.java         # 广播模式消费者
    └── TopicConsumer.java          # 主题模式消费者
```
