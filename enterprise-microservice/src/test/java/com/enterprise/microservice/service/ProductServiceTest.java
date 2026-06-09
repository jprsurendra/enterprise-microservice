package com.enterprise.microservice.service;

import com.enterprise.microservice.dto.product.ProductRequest;
import com.enterprise.microservice.dto.product.ProductResponse;
import com.enterprise.microservice.entity.Product;
import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.mapper.ProductMapper;
import com.enterprise.microservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private ProductService service;

    @Test
    void shouldCreateProductSuccessfully() {

        ProductRequest request = new ProductRequest(
                "Laptop",
                "Gaming Laptop",
                "SKU001",
                "Electronics",
                BigDecimal.valueOf(1500),
                true
        );

        Product product = new Product();

        Product saved = new Product();
        saved.setId(1L);

        ProductResponse response = new ProductResponse(
                1L,
                "Laptop",
                "Gaming Laptop",
                "SKU001",
                "Electronics",
                BigDecimal.valueOf(1500),
                true,
                null,
                null
        );

        when(repository.existsBySku("SKU001"))
                .thenReturn(false);

        when(mapper.toEntity(request))
                .thenReturn(product);

        when(repository.save(product))
                .thenReturn(saved);

        when(mapper.toResponse(saved))
                .thenReturn(response);

        ProductResponse result =
                service.createProduct(request);

        assertNotNull(result);

        assertEquals(1L, result.id());

        verify(repository).save(product);
    }

    @Test
    void shouldThrowExceptionWhenSkuExists() {

        ProductRequest request = new ProductRequest(
                "Laptop",
                "Gaming Laptop",
                "SKU001",
                "Electronics",
                BigDecimal.valueOf(1500),
                true
        );

        when(repository.existsBySku("SKU001"))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.createProduct(request));

        verify(repository, never()).save(any());
    }
}