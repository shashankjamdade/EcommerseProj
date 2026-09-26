package org.example.productservice.repository;

import org.example.productservice.model.SpecificationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpecificationRepository extends ReactiveCrudRepository<SpecificationEntity, UUID> {

    Flux<SpecificationEntity> findAllByProductIdOrderByDisplayOrderAsc(UUID productId);

    Mono<SpecificationEntity> findByIdAndProductId(UUID id, UUID productId);
}

