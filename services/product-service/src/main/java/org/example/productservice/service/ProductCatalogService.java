package org.example.productservice.service;

import org.example.productservice.dto.CategoryResponse;
import org.example.productservice.dto.ProductDetailsResponse;
import org.example.productservice.dto.ProductSummaryResponse;
import org.example.productservice.dto.ReviewCreateRequest;
import org.example.productservice.dto.ReviewResponse;
import org.example.productservice.dto.SellerResponse;
import org.example.productservice.model.ProductEntity;
import org.example.productservice.model.ReviewEntity;
import org.example.productservice.repository.CategoryRepository;
import org.example.productservice.repository.ProductImageRepository;
import org.example.productservice.repository.ProductRepository;
import org.example.productservice.repository.ReviewRepository;
import org.example.productservice.repository.SellerRepository;
import org.example.productservice.repository.SpecificationRepository;
import org.example.productservice.repository.VariantRepository;
import org.example.productservice.security.AuthenticatedUser;
import org.example.productservice.security.CacheProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductCatalogService {

    private static final String CATEGORIES_CACHE_KEY = "product-service:categories:active";
    private static final String PRODUCT_CACHE_PREFIX = "product-service:product:";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final VariantRepository variantRepository;
    private final ProductImageRepository productImageRepository;
    private final SpecificationRepository specificationRepository;
    private final ReviewRepository reviewRepository;
    private final ProductPayloadMapper payloadMapper;
    private final RedisCacheService redisCacheService;
    private final CacheProperties cacheProperties;
    private final Clock clock;

    public ProductCatalogService(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 SellerRepository sellerRepository,
                                 VariantRepository variantRepository,
                                 ProductImageRepository productImageRepository,
                                 SpecificationRepository specificationRepository,
                                 ReviewRepository reviewRepository,
                                 ProductPayloadMapper payloadMapper,
                                 RedisCacheService redisCacheService,
                                 CacheProperties cacheProperties,
                                 Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.sellerRepository = sellerRepository;
        this.variantRepository = variantRepository;
        this.productImageRepository = productImageRepository;
        this.specificationRepository = specificationRepository;
        this.reviewRepository = reviewRepository;
        this.payloadMapper = payloadMapper;
        this.redisCacheService = redisCacheService;
        this.cacheProperties = cacheProperties;
        this.clock = clock;
    }

    public Flux<CategoryResponse> listActiveCategories() {
        return redisCacheService.getList(CATEGORIES_CACHE_KEY, CategoryResponse.class)
                .flatMapMany(Flux::fromIterable)
                .switchIfEmpty(categoryRepository.findActiveCategories()
                        .map(payloadMapper::toCategoryResponse)
                        .collectList()
                        .flatMapMany(categories -> redisCacheService.put(CATEGORIES_CACHE_KEY, categories, cacheProperties.getCategoryTtl())
                                .thenMany(Flux.fromIterable(categories))));
    }

    public Flux<ProductSummaryResponse> searchProducts(String query,
                                                       UUID categoryId,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       BigDecimal minRating,
                                                       UUID sellerUserId) {
        String normalizedQuery = normalize(query);

        return productRepository.searchActiveProducts(categoryId, sellerUserId, minPrice, maxPrice, minRating, normalizedQuery)
                .flatMap(this::toProductSummaryResponse);
    }

    public Mono<ProductDetailsResponse> getProductDetails(UUID productId) {
        String cacheKey = productCacheKey(productId);
        return redisCacheService.get(cacheKey, ProductDetailsResponse.class)
                .switchIfEmpty(loadProductDetails(productId, true)
                        .flatMap(response -> redisCacheService.put(cacheKey, response, cacheProperties.getProductTtl())
                                .thenReturn(response)));
    }

    public Flux<ReviewResponse> getProductReviews(UUID productId) {
        return findActiveProduct(productId)
                .flatMapMany(product -> reviewRepository.findAllByProductIdOrderByCreatedAtDesc(product.getId())
                        .map(payloadMapper::toReviewResponse));
    }

    public Mono<ReviewResponse> addReview(UUID productId, ReviewCreateRequest request, AuthenticatedUser currentUser) {
        return findActiveProduct(productId)
                .flatMap(product -> {
                    Instant now = Instant.now(clock);
                    ReviewEntity review = new ReviewEntity(
                            UUID.randomUUID(),
                            product.getId(),
                            currentUser.email(),
                            request.rating(),
                            request.title(),
                            request.comment(),
                            now,
                            now
                    );
                    return reviewRepository.save(review)
                            .flatMap(savedReview -> refreshProductRating(product, now).thenReturn(savedReview))
                            .delayUntil(savedReview -> redisCacheService.evict(productCacheKey(productId)))
                            .map(payloadMapper::toReviewResponse);
                });
    }

    Mono<ProductDetailsResponse> getProductDetailsForAdmin(UUID productId) {
        return loadProductDetails(productId, false);
    }

    private Mono<ProductSummaryResponse> toProductSummaryResponse(ProductEntity product) {
        return Mono.zip(
                        findCategoryResponse(product.getCategoryId()),
                        findSellerResponse(product.getSellerId())
                )
                .map(tuple -> payloadMapper.toProductSummaryResponse(product, tuple.getT1(), tuple.getT2()));
    }

    private Mono<ProductDetailsResponse> loadProductDetails(UUID productId, boolean publicOnly) {
        Mono<ProductEntity> productMono = publicOnly ? findActiveProduct(productId) : findAnyProduct(productId);
        return productMono.flatMap(product -> Mono.zip(
                findCategoryResponse(product.getCategoryId()),
                findSellerResponse(product.getSellerId()),
                variantRepository.findAllByProductIdOrderByCreatedAtAsc(product.getId())
                        .filter(variant -> !publicOnly || variant.isActive())
                        .map(payloadMapper::toVariantResponse)
                        .collectList(),
                specificationRepository.findAllByProductIdOrderByDisplayOrderAsc(product.getId())
                        .map(payloadMapper::toSpecificationResponse)
                        .collectList(),
                productImageRepository.findAllByProductIdOrdered(product.getId())
                        .map(payloadMapper::toProductImageResponse)
                        .collectList(),
                reviewRepository.countByProductId(product.getId()).defaultIfEmpty(0L)
        ).map(tuple -> payloadMapper.toProductDetailsResponse(
                product,
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4(),
                tuple.getT5(),
                tuple.getT6()
        )));
    }

    private Mono<CategoryResponse> findCategoryResponse(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(payloadMapper::toCategoryResponse)
                .defaultIfEmpty(new CategoryResponse(categoryId.toString(), "Unknown", null, false, null, null));
    }

    private Mono<SellerResponse> findSellerResponse(UUID sellerUserId) {
        return sellerRepository.findById(sellerUserId)
                .map(payloadMapper::toSellerResponse)
                .defaultIfEmpty(new SellerResponse(sellerUserId.toString(), null, null, null, null, null, null, null));
    }

    private Mono<Void> refreshProductRating(ProductEntity product, Instant now) {
        return reviewRepository.averageRating(product.getId())
                .defaultIfEmpty(BigDecimal.ZERO)
                .flatMap(averageRating -> {
                    product.setRating(averageRating.setScale(2, RoundingMode.HALF_UP));
                    product.setUpdatedAt(now);
                    return productRepository.save(product).then();
                });
    }

    private Mono<ProductEntity> findAnyProduct(UUID productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }

    private Mono<ProductEntity> findActiveProduct(UUID productId) {
        return findAnyProduct(productId)
                .filter(ProductEntity::isActive)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }

    String productCacheKey(UUID productId) {
        return PRODUCT_CACHE_PREFIX + productId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
