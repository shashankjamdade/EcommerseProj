package org.example.authservice.repository;

import org.example.authservice.model.UserAccount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserAccountRepository extends ReactiveCrudRepository<UserAccount, UUID> {

    Mono<UserAccount> findByEmail(String email);
}

