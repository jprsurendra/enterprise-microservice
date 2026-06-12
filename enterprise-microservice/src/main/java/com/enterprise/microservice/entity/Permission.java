package com.enterprise.microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_perm_resource", columnList = "resource"),
        @Index(name = "idx_perm_action",   columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;               // "PRODUCT_READ", "USER_MANAGE"

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String resource;           // "PRODUCT", "USER", "ORDER"

    @Column(nullable = false, length = 50)
    private String action;             // "READ", "CREATE", "UPDATE", "DELETE", "MANAGE"

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
}