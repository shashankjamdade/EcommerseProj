package org.example.authservice.repository;

import org.example.authservice.model.AuthSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AuthSessionRepository extends ReactiveCrudRepository<AuthSession, UUID> {

	@Query("""
			SELECT * FROM auth_sessions
			WHERE user_id = :userId
			  AND active_role = :activeRole
			  AND is_expired = FALSE
			  AND logout_time IS NULL
			ORDER BY login_time DESC
			""")
	Flux<AuthSession> findOpenSessionsByUserIdAndRole(@Param("userId") UUID userId,
													  @Param("activeRole") String activeRole);
}

