package com.enterprise.microservice.repository;

import com.enterprise.microservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryAndActiveTrue(String category);

    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.price <= :maxPrice ORDER BY p.price DESC")
    List<Product> findTopProductsByCategoryAndMaxPrice(@Param("category") String category, @Param("maxPrice") BigDecimal maxPrice);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.quantity = p.quantity - :quantity WHERE p.sku = :sku AND p.quantity >= :quantity")
    int deductProductQuantity(@Param("sku") String sku, @Param("quantity") Integer quantity);

    long countByCategoryAndActiveTrue(String category);
}