package com.example.malllearning.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")  // ← 告诉 MyBatis-Plus：这个类对应数据库的 product 表
public class Product {
    @TableId(type = IdType.AUTO)  // ← 告诉 MyBatis-Plus：id 是自增主键
    private Long id;

    private String name;
    private String description;

    private BigDecimal price;  // ← 数据库的 DECIMAL 对应 Java 的 BigDecimal

    private Integer stock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}