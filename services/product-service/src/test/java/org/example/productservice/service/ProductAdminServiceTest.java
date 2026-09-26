package org.example.productservice.service;

import org.example.productservice.dto.CategoryUpsertRequest;
import org.example.productservice.dto.ProductDetailsResponse;
import org.example.productservice.dto.ProductUpsertRequest;
import org.example.productservice.dto.SellerResponse;
import org.example.productservice.model.CategoryEntity;
import org.example.productservice.model.ProductEntity;
import org.example.productservice.model.SellerEntity;
import org.example.productservice.repository.CategoryRepository;
import org.example.productservice.repository.ProductImageRepository;
import org.example.productservice.repository.ProductRepository;
import org.example.productservice.repository.ReviewRepository;
import org.example.productservice.repository.SellerRepository;
import org.example.productservice.repository.SpecificationRepository;
import org.example.productservice.repository.VariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAdminServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
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
    private ProductCatalogService productCatalogService;
    @Mock
    private RedisCacheService redisCacheService;

    private ProductAdminService productAdminService;

    @BeforeEach
    void setUp() {
        productAdminService = new ProductAdminService(
                categoryRepository,
                productRepository,
                sellerRepository,
                variantRepository,
                productImageRepository,
                specificationRepository,
                reviewRepository,
                productCatalogService,
                new ProductPayloadMapper(new com.fasterxml.jackson.databind.ObjectMapper()),
                redisCacheService,
                Clock.fixed(Instant.parse("2026-09-18T11:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateCategory() {
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(redisCacheService.evict(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(productAdminService.createCategory(new CategoryUpsertRequest("Electronics", "Devices", true)))
                .assertNext(response -> {
                    assertThat(response.name()).isEqualTo("Electronics");
                    assertThat(response.description()).isEqualTo("Devices");
                    assertThat(response.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void shouldCreateProductWhenCategoryAndSellerExist() {
        UUID categoryId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID sellerUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID productId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        CategoryEntity category = new CategoryEntity(
                categoryId,
                "Electronics",
                "Devices",
                true,
                Instant.now(),
                Instant.now()
        ).markPersisted();
        SellerEntity seller = new SellerEntity(
                sellerUserId,
                "Trusted merchant",
                "Pune, India",
                new BigDecimal("18.520430"),
                new BigDecimal("73.856743"),
                760,
                Instant.now(),
                Instant.now()
        ).markPersisted();
        ProductEntity savedProduct = new ProductEntity(
                productId,
                "Laptop Pro",
                categoryId,
                new BigDecimal("1200.00"),
                new BigDecimal("1400.00"),
                new BigDecimal("4.40"),
                "Powerful laptop",
                sellerUserId,
                true,
                Instant.now(),
                Instant.now()
        ).markPersisted();
        ProductDetailsResponse productDetailsResponse = new ProductDetailsResponse(
                productId.toString(),
                "Laptop Pro",
                null,
                new BigDecimal("1200.00"),
                new BigDecimal("1400.00"),
                new BigDecimal("4.40"),
                0L,
                "Powerful laptop",
                new SellerResponse(sellerUserId.toString(), "Trusted merchant", "Pune, India", new BigDecimal("18.520430"), new BigDecimal("73.856743"), 760, null, null),
                true,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now()
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(category));
        when(sellerRepository.findById(sellerUserId)).thenReturn(Mono.just(seller));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(Mono.just(savedProduct));
        when(productCatalogService.getProductDetailsForAdmin(productId)).thenReturn(Mono.just(productDetailsResponse));

        StepVerifier.create(productAdminService.createProduct(new ProductUpsertRequest(
                        "Laptop Pro",
                        categoryId,
                        new BigDecimal("1200.00"),
                        new BigDecimal("1400.00"),
                        new BigDecimal("4.40"),
                        "Powerful laptop",
                        sellerUserId,
                        true)))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(productId.toString());
                    assertThat(response.seller().userId()).isEqualTo(sellerUserId.toString());
                })
                .verifyComplete();
    }

    @Test
    void shouldPreventDeletingCategoryWithProducts() {
        UUID categoryId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        CategoryEntity category = new CategoryEntity(categoryId, "Mobiles", null, true, Instant.now(), Instant.now()).markPersisted();

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(category));
        when(productRepository.countByCategoryId(categoryId)).thenReturn(Mono.just(1L));
        when(redisCacheService.evict(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(productAdminService.deleteCategory(categoryId))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException exception = (ResponseStatusException) throwable;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
    }
}
