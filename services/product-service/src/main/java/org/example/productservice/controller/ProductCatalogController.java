package org.example.productservice.controller;

import jakarta.validation.Valid;
import org.example.productservice.dto.CategoryResponse;
import org.example.productservice.dto.ProductDetailsResponse;
import org.example.productservice.dto.ProductSummaryResponse;
import org.example.productservice.dto.ReviewCreateRequest;
import org.example.productservice.dto.ReviewResponse;
import org.example.productservice.security.AuthenticatedUser;
import org.example.productservice.service.ProductCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/products")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    public ProductCatalogController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping("/categories")
    public Flux<CategoryResponse> listCategories() {
        return productCatalogService.listActiveCategories();
    }

    @GetMapping
    public Flux<ProductSummaryResponse> searchProducts(@RequestParam(name = "query", required = false) String query,
                                                       @RequestParam(name = "categoryId", required = false) UUID categoryId,
                                                       @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                                                       @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                                                       @RequestParam(name = "minRating", required = false) BigDecimal minRating,
                                                       @RequestParam(name = "sellerUserId", required = false) UUID sellerUserId) {
        return productCatalogService.searchProducts(query, categoryId, minPrice, maxPrice, minRating, sellerUserId);
    }

    @GetMapping("/{productId}")
    public Mono<ProductDetailsResponse> getProductDetails(@PathVariable("productId") UUID productId) {
        return productCatalogService.getProductDetails(productId);
    }

    @GetMapping("/{productId}/reviews")
    public Flux<ReviewResponse> getProductReviews(@PathVariable("productId") UUID productId) {
        return productCatalogService.getProductReviews(productId);
    }

    @PostMapping("/{productId}/reviews")
    public Mono<ResponseEntity<ReviewResponse>> addReview(@PathVariable("productId") UUID productId,
                                                          @Valid @RequestBody ReviewCreateRequest request,
                                                          Authentication authentication) {
        return productCatalogService.addReview(productId, request, currentUser(authentication))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}

