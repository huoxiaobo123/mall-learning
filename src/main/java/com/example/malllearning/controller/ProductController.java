package com.example.malllearning.controller;

import com.example.malllearning.model.Product;
import com.example.malllearning.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

//@RestController 其实 = @Controller + @ResponseBody
//web控制器，把数据转为josn可以让浏览器接受
@RestController
@RequestMapping("/product")
public class ProductController {
    //DI注入产品服务类实例
    @Autowired
    private ProductService productService;
    //调用方法返回list
    @GetMapping("/list")//注册路由
    public List<Product> list() {
        return productService.getAllProducts();
    }
    //得到访问产品的id
    @GetMapping("/{id}")
    public Product detail(@PathVariable Long id) {
        return productService.getProductById(id);
    }
    @GetMapping("/search")
    public List<Product> search(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }
    @PostMapping("/add")
    public String add(@RequestBody Product product) {
        productService.addProduct(product);
        return "添加成功";
    }

    @PutMapping("/update")
    public String update(@RequestBody Product product) {
        productService.updateProduct(product);
        return "更新成功";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "删除成功";
    }
}