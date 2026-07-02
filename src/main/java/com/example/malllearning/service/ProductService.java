package com.example.malllearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.malllearning.mapper.ProductMapper;
import com.example.malllearning.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    // Redis 还没启动时设为非必需
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMessageSender productMessageSender;

    private final String CACHE_KEY_PREFIX = "product:";

    public List<Product> getAllProducts() {
        return productMapper.selectList(null);
    }

    public Product getProductById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;

        // Redis 可用时才走缓存
        if (redisTemplate != null) {
            Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
            if (product != null) {
                System.out.println("====== 从缓存读取商品：" + id + " ======");
                return product;
            }
            System.out.println("====== 缓存未命中，从数据库读取：" + id + " ======");
        }

        Product product = productMapper.selectById(id);

        if (product != null && redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, product, 30, TimeUnit.MINUTES);
        }

        return product;
    }

    public List<Product> searchProducts(String keyword) {
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .like(Product::getName, keyword)
                        .or()
                        .like(Product::getDescription, keyword)
        );
    }

    public void addProduct(Product product) {
        productMapper.insert(product);
        System.out.println("====== 商品新增，清除缓存 ======");
        productMessageSender.sendProductLog(product, "新增");
    }

    public void updateProduct(Product product) {
        productMapper.updateById(product);
        if (redisTemplate != null) {
            String cacheKey = CACHE_KEY_PREFIX + product.getId();
            redisTemplate.delete(cacheKey);
        }
        System.out.println("====== 商品更新，清除缓存：" + product.getId() + " ======");
        productMessageSender.sendProductLog(product, "更新");
    }

    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            System.out.println("====== 商品不存在，ID：" + id + " ======");
            return;
        }

        productMapper.deleteById(id);
        if (redisTemplate != null) {
            String cacheKey = CACHE_KEY_PREFIX + id;
            redisTemplate.delete(cacheKey);
        }
        System.out.println("====== 商品删除，清除缓存：" + id + " ======");
        productMessageSender.sendProductLog(product, "删除");
    }
}
