package com.example.shop.web;

import com.example.shop.domain.Product;
import com.example.shop.service.ProductService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> list(HttpServletRequest request) {
        return service.findAll();
    }

    @PostMapping
    public Product create(@Valid @RequestBody Product product) {
        return service.save(product);
    }
}
