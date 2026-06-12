package com.enterprise.microservice.controller;

import com.enterprise.microservice.annotation.ApiLog;
import com.enterprise.microservice.annotation.CheckPermission;
import com.enterprise.microservice.dto.ApiErrorResponse;
import com.enterprise.microservice.dto.ApiResponse;
import com.enterprise.microservice.dto.ProductDto;
import com.enterprise.microservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "Product catalogue management — CRUD operations")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "List all products (paginated)", description = "Returns a paginated list of all active products. Requires ROLE_USER or ROLE_ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })

    @GetMapping
    // @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @CheckPermission("PRODUCT_READ")      // ← dynamic, from DB
    @ApiLog(description = "List all products")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ProductDto> getProductById(
            @Parameter(description = "Product database ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Create a new product", description = "Requires ROLE_ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "SKU already exists",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @CheckPermission("PRODUCT_CREATE") // @PreAuthorize("hasRole('ADMIN')")
    @ApiLog(description = "Create product", maskFields = {"password"})
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    @Operation(summary = "Update an existing product", description = "Requires ROLE_ADMIN. SKU and active flag are immutable.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @CheckPermission("PRODUCT_UPDATE") // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @Parameter(description = "Product database ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @Operation(summary = "Soft-delete a product", description = "Sets active=false. Does not physically delete. Requires ROLE_ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @CheckPermission("PRODUCT_DELETE") // @PreAuthorize("hasRole('ADMIN')")
    @ApiLog(description = "Soft-delete product", logResponseBody = false)
    public ResponseEntity<ApiResponse> deleteProduct(
            @Parameter(description = "Product database ID", example = "1") @PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated successfully."));
    }
}