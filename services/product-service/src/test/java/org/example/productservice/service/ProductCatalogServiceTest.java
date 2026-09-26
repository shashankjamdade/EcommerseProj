package org.example.productservice.service;

import org.example.productservice.dto.ProductDetailsResponse;
import org.example.productservice.dto.ReviewCreateRequest;
import org.example.productservice.model.CategoryEntity;
import org.example.productservice.model.ProductEntity;
import org.example.productservice.model.ReviewEntity;
import org.example.productservice.model.Role;
import org.example.productservice.model.SellerEntity;
import org.example.productservice.repository.CategoryRepository;
import org.example.productservice.repository.ProductImageRepository;
import org.example.productservice.repository.ProductRepository;
import org.example.productservice.repository.ReviewRepository;
import org.example.productservice.repository.SellerRepository;
import org.example.productservice.repository.SpecificationRepository;
import org.example.productservice.repository.VariantRepository;
import org.example.productservice.security.AuthenticatedUser;
import org.example.productservice.security.CacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private VariantRepository variantRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private SpecificationRepository specificationRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RedisCacheService redisCacheService;

    private ProductCatalogService productCatalogService;

    @BeforeEach
    void setUp() {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setProductTtl(Duration.ofMinutes(10));
        cacheProperties.setCategoryTtl(Duration.ofMinutes(30));
        ProductPayloadMapper mapper = new ProductPayloadMapper(new com.fasterxml.jackson.databind.ObjectMapper());
        productCatalogService = new ProductCatalogService(
                productRepository,
                categoryRepository,
                sellerRepository,
                variantRepository,
                productImageRepository,
                specificationRepository,
                reviewRepository,
                mapper,
                redisCacheService,
                cacheProperties,
                Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldReturnProductDetailsFromDatabaseAndPopulateCache() {
        UUID categoryId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID sellerUserId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ProductEntity product = new ProductEntity(
                productId,
                "iPhone 17",
                categoryId,
                new BigDecimal("999.99"),
                new BigDecimal("1099.99"),
                new BigDecimal("4.70"),
                "Flagship phone",
                sellerUserId,
                true,
                Instant.parse("2026-09-18T09:00:00Z"),
                Instant.parse("2026-09-18T09:30:00Z")
        ).markPersisted();
        CategoryEntity category = new CategoryEntity(
                categoryId,
                "Mobiles",
                "Smartphones",
                true,
                Instant.parse("2026-09-18T08:00:00Z"),
                Instant.parse("2026-09-18T08:15:00Z")
        ).markPersisted();
        SellerEntity seller = new SellerEntity(
                sellerUserId,
                "Premium Apple reseller",
                "Cupertino, CA",
                new BigDecimal("37.331820"),
                new BigDecimal("-122.031180"),
                810,
                Instant.parse("2026-09-18T08:00:00Z"),
                Instant.parse("2026-09-18T08:20:00Z")
        ).markPersisted();

        when(redisCacheService.get(anyString(), eq(org.example.productservice.dto.ProductDetailsResponse.class))).thenReturn(Mono.empty());
        when(productRepository.findById(productId)).thenReturn(Mono.just(product));
        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(category));
        when(sellerRepository.findById(sellerUserId)).thenReturn(Mono.just(seller));
        when(variantRepository.findAllByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Flux.empty());
        when(specificationRepository.findAllByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Flux.empty());
        when(productImageRepository.findAllByProductIdOrdered(productId)).thenReturn(Flux.empty());
        when(reviewRepository.countByProductId(productId)).thenReturn(Mono.just(3L));
        when(redisCacheService.put(anyString(), any(ProductDetailsResponse.class), any())).thenReturn(Mono.empty());

        StepVerifier.create(productCatalogService.getProductDetails(productId))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(productId.toString());
                    assertThat(response.name()).isEqualTo("iPhone 17");
                    assertThat(response.category().name()).isEqualTo("Mobiles");
                    assertThat(response.seller().userId()).isEqualTo(sellerUserId.toString());
                    assertThat(response.seller().creditScore()).isEqualTo(810);
                    assertThat(response.reviewCount()).isEqualTo(3L);
                    assertThat(response.variants()).isEmpty();
                    assertThat(response.specifications()).isEmpty();
                    assertThat(response.images()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldAddReviewAndRefreshProductRating() {
        UUID productId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        ProductEntity product = new ProductEntity(
                productId,
                "Galaxy Ultra",
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                new BigDecimal("899.99"),
                null,
                new BigDecimal("4.10"),
                "Android phone",
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                true,
                Instant.parse("2026-09-18T07:00:00Z"),
                Instant.parse("2026-09-18T07:00:00Z")
        ).markPersisted();
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                "buyer@example.com",
                Set.of(Role.USER),
                Role.USER
        );

        when(productRepository.findById(productId)).thenReturn(Mono.just(product));
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(reviewRepository.averageRating(productId)).thenReturn(Mono.just(new BigDecimal("4.50")));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(redisCacheService.evict(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(productCatalogService.addReview(productId,
                        new ReviewCreateRequest(5, "Great", "Loved it"),
                        currentUser))
                .assertNext(response -> {
                    assertThat(response.productId()).isEqualTo(productId.toString());
                    assertThat(response.reviewerEmail()).isEqualTo("buyer@example.com");
                    assertThat(response.rating()).isEqualTo(5);
                    assertThat(response.title()).isEqualTo("Great");
                })
                .verifyComplete();
    }
}
