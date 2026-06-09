package com.enterprise.microservice.service;

import com.enterprise.microservice.dto.product.ProductRequest;
import com.enterprise.microservice.dto.product.ProductResponse;
import com.enterprise.microservice.dto.product.ProductUpdateRequest;
import com.enterprise.microservice.entity.Product;
import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.exception.ErrorCode;
import com.enterprise.microservice.mapper.ProductMapper;
import com.enterprise.microservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;

    private final ProductRepository productRepository;

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /*
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND, "Product not found with id: " + id));
    }
    */

    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new BusinessException(ErrorCode.ERR_DATA_VALIDATION, "Product with SKU " + product.getSku() + " already exists");
        }
        return productRepository.save(product);
    }
    /*
    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setQuantity(productDetails.getQuantity());
        product.setCategory(productDetails.getCategory());
        return productRepository.save(product);
    }
    */

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        }

        Product product =productMapper.toEntity(request);

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {

        Product product = productRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        productMapper.updateEntity(request, product);

        Product updated = productRepository.save(product);

        return productMapper.toResponse(updated);
    }
    @Transactional(readOnly = true)
    public ProductResponse getProduct(  Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }
}