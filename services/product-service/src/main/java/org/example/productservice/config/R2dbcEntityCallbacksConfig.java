package org.example.productservice.config;

import org.example.productservice.model.CategoryEntity;
import org.example.productservice.model.ProductImageEntity;
import org.example.productservice.model.ProductEntity;
import org.example.productservice.model.ReviewEntity;
import org.example.productservice.model.SellerEntity;
import org.example.productservice.model.SpecificationEntity;
import org.example.productservice.model.VariantEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import reactor.core.publisher.Mono;

@Configuration
public class R2dbcEntityCallbacksConfig {

    @Bean
    public AfterConvertCallback<CategoryEntity> categoryAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<CategoryEntity> categoryAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterConvertCallback<ProductEntity> productAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<ProductEntity> productAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterConvertCallback<SellerEntity> sellerAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<SellerEntity> sellerAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterConvertCallback<VariantEntity> variantAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<VariantEntity> variantAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterConvertCallback<ProductImageEntity> productImageAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<ProductImageEntity> productImageAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterConvertCallback<SpecificationEntity> specificationAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<SpecificationEntity> specificationAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterConvertCallback<ReviewEntity> reviewAfterConvertCallback() {
        return (entity, table) -> Mono.just(entity.markPersisted());
    }

    @Bean
    public AfterSaveCallback<ReviewEntity> reviewAfterSaveCallback() {
        return (entity, outboundRow, table) -> Mono.just(entity.markPersisted());
    }
}

