package org.example.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.productservice.dto.CategoryResponse;
import org.example.productservice.dto.LabelValuePair;
import org.example.productservice.dto.ProductDetailsResponse;
import org.example.productservice.dto.ProductImageResponse;
import org.example.productservice.dto.ProductSummaryResponse;
import org.example.productservice.dto.ReviewResponse;
import org.example.productservice.dto.SellerResponse;
import org.example.productservice.dto.SpecificationResponse;
import org.example.productservice.dto.VariantResponse;
import org.example.productservice.model.CategoryEntity;
import org.example.productservice.model.ProductImageEntity;
import org.example.productservice.model.ProductEntity;
import org.example.productservice.model.ReviewEntity;
import org.example.productservice.model.SellerEntity;
import org.example.productservice.model.SpecificationEntity;
import org.example.productservice.model.VariantEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductPayloadMapper {

    private static final TypeReference<List<LabelValuePair>> ATTRIBUTES_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ProductPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CategoryResponse toCategoryResponse(CategoryEntity entity) {
        return new CategoryResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public VariantResponse toVariantResponse(VariantEntity entity) {
        return new VariantResponse(
                entity.getId().toString(),
                entity.getProductId().toString(),
                entity.getName(),
                entity.getSku(),
                entity.getPrice(),
                entity.getOldPrice(),
                entity.getStockQuantity(),
                toAttributes(entity.getAttributesJson()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SpecificationResponse toSpecificationResponse(SpecificationEntity entity) {
        return new SpecificationResponse(
                entity.getId().toString(),
                entity.getProductId().toString(),
                entity.getLabel(),
                entity.getValue(),
                entity.getUnit(),
                entity.getDisplayOrder()
        );
    }

    public SellerResponse toSellerResponse(SellerEntity entity) {
        return new SellerResponse(
                entity.getUserId().toString(),
                entity.getDescription(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getCreditScore(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductImageResponse toProductImageResponse(ProductImageEntity entity) {
        return new ProductImageResponse(
                entity.getId().toString(),
                entity.getProductId().toString(),
                entity.getImageUrl(),
                entity.getAltText(),
                entity.isPrimary(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ReviewResponse toReviewResponse(ReviewEntity entity) {
        return new ReviewResponse(
                entity.getId().toString(),
                entity.getProductId().toString(),
                entity.getReviewerEmail(),
                entity.getRating(),
                entity.getTitle(),
                entity.getComment(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductSummaryResponse toProductSummaryResponse(ProductEntity product,
                                                           CategoryResponse category,
                                                           SellerResponse seller) {
        return new ProductSummaryResponse(
                product.getId().toString(),
                product.getName(),
                product.getCategoryId().toString(),
                category == null ? null : category.name(),
                product.getPrice(),
                product.getOldPrice(),
                product.getRating(),
                seller,
                product.getDescription()
        );
    }

    public ProductDetailsResponse toProductDetailsResponse(ProductEntity product,
                                                           CategoryResponse category,
                                                           SellerResponse seller,
                                                           List<VariantResponse> variants,
                                                           List<SpecificationResponse> specifications,
                                                           List<ProductImageResponse> images,
                                                           long reviewCount) {
        return new ProductDetailsResponse(
                product.getId().toString(),
                product.getName(),
                category,
                product.getPrice(),
                product.getOldPrice(),
                product.getRating(),
                reviewCount,
                product.getDescription(),
                seller,
                product.isActive(),
                variants,
                specifications,
                images,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public String toAttributesJson(List<LabelValuePair> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes == null ? List.of() : attributes);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize variant attributes", exception);
        }
    }

    public List<LabelValuePair> toAttributes(String attributesJson) {
        if (attributesJson == null || attributesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(attributesJson, ATTRIBUTES_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to deserialize variant attributes", exception);
        }
    }
}

