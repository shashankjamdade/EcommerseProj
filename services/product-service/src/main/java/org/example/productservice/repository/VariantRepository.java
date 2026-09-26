package org.example.productservice.repository;

import org.example.productservice.model.VariantEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface VariantRepository extends ReactiveCrudRepository<VariantEntity, UUID> {

    Flux<VariantEntity> findAllByProductIdOrderByCreatedAtAsc(UUID productId);

    Mono<VariantEntity> findByIdAndProductId(UUID id, UUID productId);
}

