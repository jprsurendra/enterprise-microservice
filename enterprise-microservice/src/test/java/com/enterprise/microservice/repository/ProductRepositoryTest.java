package com.enterprise.microservice.repository;

import com.enterprise.microservice.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldFindBySku() {

        Product product = new Product();

        product.setName("Phone");

        product.setSku("SKU123");

        product.setCategory("Electronics");

        product.setPrice(BigDecimal.TEN);

        product.setActive(true);

        repository.save(product);

        assertTrue(
                repository.findBySku("SKU123")
                        .isPresent());
    }
}