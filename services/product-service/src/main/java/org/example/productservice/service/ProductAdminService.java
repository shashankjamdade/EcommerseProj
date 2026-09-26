package org.example.productservice.service;

import org.example.productservice.dto.CategoryResponse;
import org.example.productservice.dto.CategoryUpsertRequest;
import org.example.productservice.dto.ProductDetailsResponse;
import org.example.productservice.dto.ProductImageResponse;
import org.example.productservice.dto.ProductImageUpsertRequest;
import org.example.productservice.dto.ProductSummaryResponse;
import org.example.productservice.dto.ProductUpsertRequest;
import org.example.productservice.dto.ReviewResponse;
import org.example.productservice.dto.SellerResponse;
import org.example.productservice.dto.SellerUpsertRequest;
import org.example.productservice.dto.SpecificationResponse;
import org.example.productservice.dto.SpecificationUpsertRequest;
import org.example.productservice.dto.VariantResponse;
import org.example.productservice.dto.VariantUpsertRequest;
import org.example.productservice.model.CategoryEntity;
import org.example.productservice.model.ProductEntity;
import org.example.productservice.model.ProductImageEntity;
import org.example.productservice.model.SellerEntity;
import org.example.productservice.model.SpecificationEntity;
import org.example.productservice.model.VariantEntity;
import org.example.productservice.repository.CategoryRepository;
import org.example.productservice.repository.ProductImageRepository;
import org.example.productservice.repository.ProductRepository;
import org.example.productservice.repository.ReviewRepository;
import org.example.productservice.repository.SellerRepository;
import org.example.productservice.repository.SpecificationRepository;
import org.example.productservice.repository.VariantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductAdminService {

    private static final String ACTIVE_CATEGORIES_CACHE_KEY = "product-service:categories:active";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final VariantRepository variantRepository;
    private final ProductImageRepository productImageRepository;
    private final SpecificationRepository specificationRepository;
    private final ReviewRepository reviewRepository;
    private final ProductCatalogService productCatalogService;
    private final ProductPayloadMapper payloadMapper;
    private final RedisCacheService redisCacheService;
    private final Clock clock;

    public ProductAdminService(CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               SellerRepository sellerRepository,
                               VariantRepository variantRepository,
                               ProductImageRepository productImageRepository,
                               SpecificationRepository specificationRepository,
                               ReviewRepository reviewRepository,
                               ProductCatalogService productCatalogService,
                               ProductPayloadMapper payloadMapper,
                               RedisCacheService redisCacheService,
                               Clock clock) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
        this.variantRepository = variantRepository;
        this.productImageRepository = productImageRepository;
        this.specificationRepository = specificationRepository;
        this.reviewRepository = reviewRepository;
        this.productCatalogService = productCatalogService;
        this.payloadMapper = payloadMapper;
        this.redisCacheService = redisCacheService;
        this.clock = clock;
    }

    public Flux<SellerResponse> listSellers() {
        return sellerRepository.findAllOrdered().map(payloadMapper::toSellerResponse);
    }

    public Mono<SellerResponse> createSeller(SellerUpsertRequest request) {
        Instant now = Instant.now(clock);
        SellerEntity entity = new SellerEntity(
                request.userId(),
                normalizeOptionalText(request.description()),
                normalizeOptionalText(request.address()),
                request.latitude(),
                request.longitude(),
                request.creditScore(),
                now,
                now
        );
        return sellerRepository.save(entity)
                .map(payloadMapper::toSellerResponse)
                .onErrorMap(DataIntegrityViolationException.class,
                        exception -> new ResponseStatusException(HttpStatus.CONFLICT, "Seller already exists", exception));
    }

    public Mono<SellerResponse> getSeller(UUID sellerUserId) {
        return findSeller(sellerUserId).map(payloadMapper::toSellerResponse);
    }

    public Mono<SellerResponse> updateSeller(UUID sellerUserId, SellerUpsertRequest request) {
        validateSellerUserId(sellerUserId, request);
        return findSeller(sellerUserId)
                .flatMap(existing -> {
                    existing.setDescription(normalizeOptionalText(request.description()));
                    existing.setAddress(normalizeOptionalText(request.address()));
                    existing.setLatitude(request.latitude());
                    existing.setLongitude(request.longitude());
                    existing.setCreditScore(request.creditScore());
                    existing.setUpdatedAt(Instant.now(clock));
                    return sellerRepository.save(existing);
                })
                .map(payloadMapper::toSellerResponse);
    }

    public Mono<Void> deleteSeller(UUID sellerUserId) {
        return findSeller(sellerUserId)
                .flatMap(seller -> productRepository.countBySellerId(sellerUserId)
                        .flatMap(count -> count > 0
                                ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot delete seller with existing products"))
                                : sellerRepository.delete(seller)));
    }

    public Flux<CategoryResponse> listCategories() {
        return categoryRepository.findAllOrdered().map(payloadMapper::toCategoryResponse);
    }

    public Mono<CategoryResponse> createCategory(CategoryUpsertRequest request) {
        Instant now = Instant.now(clock);
        CategoryEntity entity = new CategoryEntity(
                UUID.randomUUID(),
                request.name().trim(),
                normalizeOptionalText(request.description()),
                request.active() == null || request.active(),
                now,
                now
        );
        return categoryRepository.save(entity)
                .delayUntil(saved -> redisCacheService.evict(ACTIVE_CATEGORIES_CACHE_KEY))
                .map(payloadMapper::toCategoryResponse)
                .onErrorMap(DataIntegrityViolationException.class,
                        exception -> new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists", exception));
    }

    public Mono<CategoryResponse> updateCategory(UUID categoryId, CategoryUpsertRequest request) {
        return findCategory(categoryId)
                .flatMap(existing -> {
                    existing.setName(request.name().trim());
                    existing.setDescription(normalizeOptionalText(request.description()));
                    existing.setActive(request.active() == null || request.active());
                    existing.setUpdatedAt(Instant.now(clock));
                    return categoryRepository.save(existing);
                })
                .delayUntil(saved -> redisCacheService.evict(ACTIVE_CATEGORIES_CACHE_KEY))
                .map(payloadMapper::toCategoryResponse)
                .onErrorMap(DataIntegrityViolationException.class,
                        exception -> new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists", exception));
    }

    public Mono<Void> deleteCategory(UUID categoryId) {
        return findCategory(categoryId)
                .flatMap(category -> productRepository.countByCategoryId(categoryId)
                        .flatMap(count -> count > 0
                                ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot delete category with existing products"))
                                : categoryRepository.delete(category)))
                .then(redisCacheService.evict(ACTIVE_CATEGORIES_CACHE_KEY));
    }

    public Flux<ProductSummaryResponse> listProducts() {
        return productRepository.findAllOrdered()
                .flatMap(product -> Mono.zip(
                                findCategoryResponse(product.getCategoryId()),
                                findSellerResponse(product.getSellerId())
                        )
                        .map(tuple -> payloadMapper.toProductSummaryResponse(product, tuple.getT1(), tuple.getT2())));
    }

    public Mono<ProductDetailsResponse> createProduct(ProductUpsertRequest request) {
        return Mono.zip(findCategory(request.categoryId()), findSeller(request.sellerUserId()))
                .flatMap(tuple -> {
                    Instant now = Instant.now(clock);
                    ProductEntity entity = new ProductEntity(
                            UUID.randomUUID(),
                            request.name().trim(),
                            tuple.getT1().getId(),
                            request.price(),
                            request.oldPrice(),
                            request.rating() == null ? BigDecimal.ZERO.setScale(2) : request.rating(),
                            normalizeOptionalText(request.description()),
                            tuple.getT2().getUserId(),
                            request.active() == null || request.active(),
                            now,
                            now
                    );
                    return productRepository.save(entity);
                })
                .flatMap(product -> productCatalogService.getProductDetailsForAdmin(product.getId()));
    }

    public Mono<ProductDetailsResponse> getProduct(UUID productId) {
        return productCatalogService.getProductDetailsForAdmin(productId);
    }

    public Mono<ProductDetailsResponse> updateProduct(UUID productId, ProductUpsertRequest request) {
        return Mono.zip(findAnyProduct(productId), findCategory(request.categoryId()), findSeller(request.sellerUserId()))
                .flatMap(tuple -> {
                    ProductEntity existing = tuple.getT1();
                    existing.setName(request.name().trim());
                    existing.setCategoryId(tuple.getT2().getId());
                    existing.setPrice(request.price());
                    existing.setOldPrice(request.oldPrice());
                    existing.setRating(request.rating() == null ? existing.getRating() : request.rating());
                    existing.setDescription(normalizeOptionalText(request.description()));
                    existing.setSellerId(tuple.getT3().getUserId());
                    existing.setActive(request.active() == null || request.active());
                    existing.setUpdatedAt(Instant.now(clock));
                    return productRepository.save(existing);
                })
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(saved.getId())))
                .flatMap(product -> productCatalogService.getProductDetailsForAdmin(product.getId()));
    }

    public Mono<Void> deleteProduct(UUID productId) {
        return findAnyProduct(productId)
                .flatMap(productRepository::delete)
                .then(redisCacheService.evict(productCatalogService.productCacheKey(productId)));
    }

    public Flux<VariantResponse> listVariants(UUID productId) {
        return findAnyProduct(productId)
                .flatMapMany(product -> variantRepository.findAllByProductIdOrderByCreatedAtAsc(productId)
                        .map(payloadMapper::toVariantResponse));
    }

    public Mono<VariantResponse> createVariant(UUID productId, VariantUpsertRequest request) {
        return findAnyProduct(productId)
                .flatMap(product -> {
                    Instant now = Instant.now(clock);
                    VariantEntity variant = new VariantEntity(
                            UUID.randomUUID(),
                            product.getId(),
                            request.name().trim(),
                            request.sku().trim(),
                            request.price(),
                            request.oldPrice(),
                            request.stockQuantity(),
                            payloadMapper.toAttributesJson(request.attributes()),
                            request.active() == null || request.active(),
                            now,
                            now
                    );
                    return variantRepository.save(variant);
                })
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(productId)))
                .map(payloadMapper::toVariantResponse)
                .onErrorMap(DataIntegrityViolationException.class,
                        exception -> new ResponseStatusException(HttpStatus.CONFLICT, "Variant SKU already exists", exception));
    }

    public Mono<VariantResponse> updateVariant(UUID productId, UUID variantId, VariantUpsertRequest request) {
        return variantRepository.findByIdAndProductId(variantId, productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found")))
                .flatMap(existing -> {
                    existing.setName(request.name().trim());
                    existing.setSku(request.sku().trim());
                    existing.setPrice(request.price());
                    existing.setOldPrice(request.oldPrice());
                    existing.setStockQuantity(request.stockQuantity());
                    existing.setAttributesJson(payloadMapper.toAttributesJson(request.attributes()));
                    existing.setActive(request.active() == null || request.active());
                    existing.setUpdatedAt(Instant.now(clock));
                    return variantRepository.save(existing);
                })
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(productId)))
                .map(payloadMapper::toVariantResponse)
                .onErrorMap(DataIntegrityViolationException.class,
                        exception -> new ResponseStatusException(HttpStatus.CONFLICT, "Variant SKU already exists", exception));
    }

    public Mono<Void> deleteVariant(UUID productId, UUID variantId) {
        return variantRepository.findByIdAndProductId(variantId, productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found")))
                .flatMap(variantRepository::delete)
                .then(redisCacheService.evict(productCatalogService.productCacheKey(productId)));
    }

    public Flux<ProductImageResponse> listImages(UUID productId) {
        return findAnyProduct(productId)
                .flatMapMany(product -> productImageRepository.findAllByProductIdOrdered(productId)
                        .map(payloadMapper::toProductImageResponse));
    }

    public Mono<ProductImageResponse> createImage(UUID productId, ProductImageUpsertRequest request) {
        return findAnyProduct(productId)
                .flatMap(product -> {
                    Instant now = Instant.now(clock);
                    ProductImageEntity image = new ProductImageEntity(
                            UUID.randomUUID(),
                            product.getId(),
                            request.imageUrl().trim(),
                            normalizeOptionalText(request.altText()),
                            Boolean.TRUE.equals(request.primary()),
                            request.displayOrder(),
                            now,
                            now
                    );
                    Mono<Void> primaryReset = image.isPrimary()
                            ? clearExistingPrimaryImages(productId, null, now)
                            : Mono.empty();
                    return primaryReset.then(productImageRepository.save(image));
                })
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(productId)))
                .map(payloadMapper::toProductImageResponse);
    }

    public Mono<ProductImageResponse> updateImage(UUID productId, UUID imageId, ProductImageUpsertRequest request) {
        return productImageRepository.findByIdAndProductId(imageId, productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product image not found")))
                .flatMap(existing -> {
                    Instant now = Instant.now(clock);
                    existing.setImageUrl(request.imageUrl().trim());
                    existing.setAltText(normalizeOptionalText(request.altText()));
                    existing.setPrimary(Boolean.TRUE.equals(request.primary()));
                    existing.setDisplayOrder(request.displayOrder());
                    existing.setUpdatedAt(now);
                    Mono<Void> primaryReset = existing.isPrimary()
                            ? clearExistingPrimaryImages(productId, imageId, now)
                            : Mono.empty();
                    return primaryReset.then(productImageRepository.save(existing));
                })
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(productId)))
                .map(payloadMapper::toProductImageResponse);
    }

    public Mono<Void> deleteImage(UUID productId, UUID imageId) {
        return productImageRepository.findByIdAndProductId(imageId, productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product image not found")))
                .flatMap(productImageRepository::delete)
                .then(redisCacheService.evict(productCatalogService.productCacheKey(productId)));
    }

    public Flux<SpecificationResponse> listSpecifications(UUID productId) {
        return findAnyProduct(productId)
                .flatMapMany(product -> specificationRepository.findAllByProductIdOrderByDisplayOrderAsc(productId)
                        .map(payloadMapper::toSpecificationResponse));
    }

    public Mono<SpecificationResponse> createSpecification(UUID productId, SpecificationUpsertRequest request) {
        return findAnyProduct(productId)
                .flatMap(product -> specificationRepository.save(new SpecificationEntity(
                        UUID.randomUUID(),
                        product.getId(),
                        request.label().trim(),
                        request.value(),
                        request.unit(),
                        request.displayOrder()
                )))
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(productId)))
                .map(payloadMapper::toSpecificationResponse);
    }

    public Mono<SpecificationResponse> updateSpecification(UUID productId,
                                                           UUID specificationId,
                                                           SpecificationUpsertRequest request) {
        return specificationRepository.findByIdAndProductId(specificationId, productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Specification not found")))
                .flatMap(existing -> {
                    existing.setLabel(request.label().trim());
                    existing.setValue(request.value());
                    existing.setUnit(request.unit());
                    existing.setDisplayOrder(request.displayOrder());
                    return specificationRepository.save(existing);
                })
                .delayUntil(saved -> redisCacheService.evict(productCatalogService.productCacheKey(productId)))
                .map(payloadMapper::toSpecificationResponse);
    }

    public Mono<Void> deleteSpecification(UUID productId, UUID specificationId) {
        return specificationRepository.findByIdAndProductId(specificationId, productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Specification not found")))
                .flatMap(specificationRepository::delete)
                .then(redisCacheService.evict(productCatalogService.productCacheKey(productId)));
    }

    public Flux<ReviewResponse> listReviews(UUID productId) {
        return findAnyProduct(productId)
                .flatMapMany(product -> reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId)
                        .map(payloadMapper::toReviewResponse));
    }

    public Mono<Void> deleteReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found")))
                .flatMap(review -> reviewRepository.delete(review)
                        .then(findAnyProduct(review.getProductId())
                                .flatMap(product -> reviewRepository.averageRating(product.getId())
                                        .defaultIfEmpty(BigDecimal.ZERO)
                                        .flatMap(averageRating -> {
                                            product.setRating(averageRating);
                                            product.setUpdatedAt(Instant.now(clock));
                                            return productRepository.save(product);
                                        }))
                                .then(redisCacheService.evict(productCatalogService.productCacheKey(review.getProductId())))))
                .then();
    }

    private Mono<Void> clearExistingPrimaryImages(UUID productId, UUID excludedImageId, Instant now) {
        return productImageRepository.findAllByProductIdOrdered(productId)
                .filter(ProductImageEntity::isPrimary)
                .filter(image -> excludedImageId == null || !image.getId().equals(excludedImageId))
                .flatMap(image -> {
                    image.setPrimary(false);
                    image.setUpdatedAt(now);
                    return productImageRepository.save(image);
                })
                .then();
    }

    private Mono<CategoryEntity> findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")));
    }

    private Mono<SellerEntity> findSeller(UUID sellerUserId) {
        return sellerRepository.findById(sellerUserId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found")));
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

    private Mono<ProductEntity> findAnyProduct(UUID productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }

    private void validateSellerUserId(UUID sellerUserId, SellerUpsertRequest request) {
        if (!sellerUserId.equals(request.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seller userId in path and payload must match");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
