package com.example.shop.service;

import com.example.shop.domain.Product;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.PostConstruct;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final List<Product> products = new CopyOnWriteArrayList<>();
    private final Tracer tracer;

    public ProductService(Tracer tracer) {
        this.tracer = tracer;
    }

    @PostConstruct
    void seed() {
        Product p = new Product();
        p.setName("Sample");
        products.add(p);
    }

    public List<Product> findAll() {
        return products;
    }

    public Product save(Product product) {
        products.add(product);
        return product;
    }
}
