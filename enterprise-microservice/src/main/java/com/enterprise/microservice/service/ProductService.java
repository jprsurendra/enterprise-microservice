package com.enterprise.microservice.service;

import com.enterprise.microservice.dto.ProductDto;
import com.enterprise.microservice.entity.Product;
import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.exception.ErrorCode;
import com.enterprise.microservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        return productRepository.findById(id)
                .filter(Boolean.TRUE::equals)  // only active
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "Product not found with id: " + id));
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        productRepository.findBySkuAndActiveTrue(dto.getSku()).ifPresent(p -> {
            throw new BusinessException(ErrorCode.ERR_DATA_CONFLICT,
                    "Product with SKU '" + dto.getSku() + "' already exists.");
        });
        Product saved = productRepository.save(toEntity(dto));
        log.info("Created product id={} sku={}", saved.getId(), saved.getSku());
        return toDto(saved);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "Product not found with id: " + id));

        // Explicit field mapping — SKU and active are intentionally immutable via this endpoint
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        product.setCategory(dto.getCategory());

        log.info("Updated product id={}", id);
        return toDto(productRepository.save(product));
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Soft-deleted product id={}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public boolean deductInventory(String sku, int quantity) {
        int rowsUpdated = productRepository.deductInventory(sku, quantity);
        if (rowsUpdated == 0) {
            log.warn("Inventory deduction failed — sku={} requestedQty={}", sku, quantity);
        }
        return rowsUpdated > 0;
    }

    // -----------------------------------------------------------------------
    // Entity <-> DTO mapping (replace with MapStruct in production for scale)
    // -----------------------------------------------------------------------

    private ProductDto toDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .description(p.getDescription())
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .category(p.getCategory())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private Product toEntity(ProductDto dto) {
        return new Product(
                dto.getName(),
                dto.getSku(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getQuantity(),
                dto.getCategory()
        );
    }
}