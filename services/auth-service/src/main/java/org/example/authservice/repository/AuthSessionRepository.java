package org.example.authservice.repository;

import org.example.authservice.model.AuthSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface AuthSessionRepository extends ReactiveCrudRepository<AuthSession, UUID> {
}

