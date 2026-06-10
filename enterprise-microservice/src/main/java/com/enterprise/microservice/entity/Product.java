package com.enterprise.microservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity: Product.
 *
 * Deliberately uses @Getter + @Setter instead of @Data.
 * @Data on JPA entities causes:
 *   1. equals/hashCode based on all mutable fields — breaks detached-entity sets.
 *   2. @ToString with lazy-loaded associations — triggers N+1 / LazyInitializationException.
 * equals/hashCode are implemented on the stable 'id' field only (null-safe for transient entities).
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_sku",      columnList = "sku",      unique = true),
        @Index(name = "idx_products_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Convenience constructor (used in service layer)
    public Product(String name, String sku, String description,
                   BigDecimal price, Integer quantity, String category) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product that)) return false;
        // Null-safe: transient entities (id == null) are never equal to each other
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Fixed hash for transient entities — safe for Hibernate's identity map
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", sku='" + sku + "', name='" + name + "'}";
    }
}