package org.example.productservice.repository;

import org.example.productservice.model.SellerEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface SellerRepository extends ReactiveCrudRepository<SellerEntity, UUID> {

    @Query("SELECT * FROM sellers ORDER BY updated_at DESC, user_id ASC")
    Flux<SellerEntity> findAllOrdered();
}
