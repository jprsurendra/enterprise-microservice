package com.enterprise.microservice.controller;

import com.enterprise.microservice.dto.ApiResponse;
import com.enterprise.microservice.dto.ProductDto;
import com.enterprise.microservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id,
                                                    @Valid @RequestBody ProductDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated successfully."));
    }
}