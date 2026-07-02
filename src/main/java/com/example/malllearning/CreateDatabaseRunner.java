package com.example.malllearning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 临时工具类：创建 mall_learning 数据库
 * 运行方式：在项目根目录执行 mvn compile 后，
 * 用 java -cp target/classes:$(find ~/.m2/repository/com/mysql -name "mysql-connector-j-9*.jar" | head -1) com.example.malllearning.CreateDatabaseRunner
 */
public class CreateDatabaseRunner {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306?useSSL=false&serverTimezone=Asia/Shanghai",
                "root", "1234");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE DATABASE IF NOT EXISTS mall_learning DEFAULT CHARACTER SET utf8mb4");
            System.out.println("Database mall_learning created successfully!");

            stmt.execute("CREATE TABLE IF NOT EXISTS mall_learning.product (" +
                    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  name VARCHAR(100) NOT NULL," +
                    "  description TEXT," +
                    "  price DECIMAL(10,2) NOT NULL DEFAULT 0," +
                    "  stock INT NOT NULL DEFAULT 0," +
                    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") DEFAULT CHARSET=utf8mb4");
            System.out.println("Table product created successfully!");

            // 插入一些示例数据
            stmt.execute("INSERT IGNORE INTO mall_learning.product (id, name, description, price, stock) VALUES " +
                    "(1, 'iPhone 16 Pro', '最新款苹果手机', 9999.00, 100)," +
                    "(2, 'MacBook Air M4', '轻薄笔记本电脑', 8999.00, 50)," +
                    "(3, 'AirPods Pro 3', '降噪耳机', 1999.00, 200)");
            System.out.println("Sample data inserted!");
        }
        System.out.println("Done!");
    }
}
