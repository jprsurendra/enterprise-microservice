package com.enterprise.microservice.repository;

import com.enterprise.microservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- Derived query methods ---

    Optional<Product> findBySkuAndActiveTrue(String sku);

    List<Product> findByCategoryAndActiveTrue(String category);

    Page<Product> findByActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    long countByCategoryAndActiveTrue(String category);

    // --- Custom JPQL queries ---

    /**
     * Returns active products in a category priced at or below the limit,
     * sorted by price descending. Used for "best value in category" queries.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.category = :category
              AND p.price <= :maxPrice
              AND p.active = true
            ORDER BY p.price DESC
            """)
    List<Product> findActiveByCategoryWithMaxPrice(
            @Param("category") String category,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * Atomic inventory deduction — only succeeds if sufficient stock exists.
     * Returns the number of rows updated (1 = success, 0 = insufficient stock or SKU not found).
     *
     * NOTE: @Transactional deliberately NOT placed here.
     * Transaction management belongs in the service layer (ProductService),
     * which owns the business transaction boundary.
     */
    @Modifying
    @Query("""
            UPDATE Product p
            SET p.quantity = p.quantity - :quantity
            WHERE p.sku = :sku
              AND p.quantity >= :quantity
              AND p.active = true
            """)
    int deductInventory(@Param("sku") String sku, @Param("quantity") Integer quantity);
}