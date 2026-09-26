package org.example.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class RedisCacheService {

    private static final Duration REDIS_OPERATION_TIMEOUT = Duration.ofMillis(500);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Mono<T> get(String key, Class<T> type) {
        return redisTemplate.opsForValue().get(key)
                .timeout(REDIS_OPERATION_TIMEOUT)
                .flatMap(value -> Mono.fromCallable(() -> objectMapper.readValue(value, type)))
                .onErrorResume(exception -> Mono.<T>empty());
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<List<T>> getList(String key, Class<T> elementType) {
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return redisTemplate.opsForValue().get(key)
                .timeout(REDIS_OPERATION_TIMEOUT)
                .flatMap(value -> Mono.fromCallable(() -> (List<T>) objectMapper.readValue(value, javaType)))
                .onErrorResume(exception -> Mono.<List<T>>empty());
    }

    public Mono<Void> put(String key, Object value, Duration ttl) {
        return Mono.fromCallable(() -> writeValue(value))
                .flatMap(payload -> redisTemplate.opsForValue().set(key, payload, ttl)
                        .timeout(REDIS_OPERATION_TIMEOUT))
                .onErrorResume(exception -> Mono.just(false))
                .then();
    }

    public Mono<Void> evict(String key) {
        return redisTemplate.delete(key)
                .timeout(REDIS_OPERATION_TIMEOUT)
                .onErrorResume(exception -> Mono.just(0L))
                .then();
    }

    public Mono<Void> evictByPattern(String pattern) {
        return redisTemplate.keys(pattern)
                .timeout(REDIS_OPERATION_TIMEOUT)
                .collectList()
                .filter(keys -> !keys.isEmpty())
                .flatMap(keys -> redisTemplate.delete(Flux.fromIterable(keys))
                        .timeout(REDIS_OPERATION_TIMEOUT))
                .onErrorResume(exception -> Mono.just(0L))
                .then();
    }

    private String writeValue(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}

