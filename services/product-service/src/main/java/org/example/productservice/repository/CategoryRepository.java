package org.example.productservice.repository;

import org.example.productservice.model.CategoryEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CategoryRepository extends ReactiveCrudRepository<CategoryEntity, UUID> {

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    Mono<CategoryEntity> findByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT * FROM categories WHERE active = TRUE ORDER BY name ASC")
    Flux<CategoryEntity> findActiveCategories();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    Flux<CategoryEntity> findAllOrdered();
}

