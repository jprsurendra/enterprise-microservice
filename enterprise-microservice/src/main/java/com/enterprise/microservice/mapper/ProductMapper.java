package com.enterprise.microservice.mapper;

import com.enterprise.microservice.dto.product.ProductRequest;
import com.enterprise.microservice.dto.product.ProductResponse;
import com.enterprise.microservice.dto.product.ProductUpdateRequest;
import com.enterprise.microservice.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    void updateEntity(ProductUpdateRequest request,
                      @MappingTarget Product product);
}