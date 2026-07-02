# Mall-Learning 🛒

基于 **Spring Boot + MyBatis-Plus + Redis + RabbitMQ** 的电商后端项目。

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

### RabbitMQ 消息队列

| 功能 | 对应文件 | 说明 |
|------|---------|------|
| Direct 直连模式 | `RabbitMQConfig.java` | 基础消息路由 |
| 死信队列 DLQ | `DeadLetterConsumer.java` | 消息处理失败机制 |
| 延迟队列/订单超时取消 | `AdvancedRabbitMQController.java` | 延迟消息应用 |
| Fanout 广播模式 | `FanoutConsumer.java` | 广播消息 |
| Topic 通配符模式 | `TopicConsumer.java` | 通配符匹配路由 |
| 幂等性处理 | `ProductMessageReceiver.java` | 防止重复消费 |
| 手动 ACK | `DeadLetterConsumer.java` | 消息确认机制 |

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
    ├── BusinessConsumer.java       # 业务队列消费者
    ├── DeadLetterConsumer.java     # 死信队列消费者
    ├── FanoutConsumer.java         # 广播模式消费者
    └── TopicConsumer.java          # 主题模式消费者
```
