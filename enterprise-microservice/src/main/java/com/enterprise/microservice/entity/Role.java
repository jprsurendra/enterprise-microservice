package com.enterprise.microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;   // stored as "ROLE_USER", "ROLE_ADMIN"

    @Column(length = 255)
    private String description;

    // Roles own their permissions — EAGER because they are always
    // needed when building the authority set for a logged-in user
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name               = "role_permissions",
            joinColumns        = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public Role(String name) { this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Role{id=" + id + ", name='" + name + "'}";
    }
}