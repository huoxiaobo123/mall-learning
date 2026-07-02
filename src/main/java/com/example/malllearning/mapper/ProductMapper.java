package com.example.malllearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.malllearning.model.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 继承 BaseMapper<Product> 后，自动拥有：
    // insert()、deleteById()、updateById()、selectById()、selectList() ...
}
