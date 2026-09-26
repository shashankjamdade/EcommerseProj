package org.example.productservice.controller;

import jakarta.validation.Valid;
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
import org.example.productservice.service.ProductAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/products/admin")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    public ProductAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping("/sellers")
    public Flux<SellerResponse> listSellers() {
        return productAdminService.listSellers();
    }

    @PostMapping("/sellers")
    public Mono<ResponseEntity<SellerResponse>> createSeller(@Valid @RequestBody SellerUpsertRequest request) {
        return productAdminService.createSeller(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/sellers/{sellerUserId}")
    public Mono<SellerResponse> getSeller(@PathVariable("sellerUserId") UUID sellerUserId) {
        return productAdminService.getSeller(sellerUserId);
    }

    @PutMapping("/sellers/{sellerUserId}")
    public Mono<SellerResponse> updateSeller(@PathVariable("sellerUserId") UUID sellerUserId,
                                             @Valid @RequestBody SellerUpsertRequest request) {
        return productAdminService.updateSeller(sellerUserId, request);
    }

    @DeleteMapping("/sellers/{sellerUserId}")
    public Mono<ResponseEntity<Void>> deleteSeller(@PathVariable("sellerUserId") UUID sellerUserId) {
        return productAdminService.deleteSeller(sellerUserId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/categories")
    public Flux<CategoryResponse> listCategories() {
        return productAdminService.listCategories();
    }

    @PostMapping("/categories")
    public Mono<ResponseEntity<CategoryResponse>> createCategory(@Valid @RequestBody CategoryUpsertRequest request) {
        return productAdminService.createCategory(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/categories/{categoryId}")
    public Mono<CategoryResponse> updateCategory(@PathVariable("categoryId") UUID categoryId,
                                                 @Valid @RequestBody CategoryUpsertRequest request) {
        return productAdminService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/categories/{categoryId}")
    public Mono<ResponseEntity<Void>> deleteCategory(@PathVariable("categoryId") UUID categoryId) {
        return productAdminService.deleteCategory(categoryId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping
    public Flux<ProductSummaryResponse> listProducts() {
        return productAdminService.listProducts();
    }

    @PostMapping
    public Mono<ResponseEntity<ProductDetailsResponse>> createProduct(@Valid @RequestBody ProductUpsertRequest request) {
        return productAdminService.createProduct(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{productId}")
    public Mono<ProductDetailsResponse> getProduct(@PathVariable("productId") UUID productId) {
        return productAdminService.getProduct(productId);
    }

    @PutMapping("/{productId}")
    public Mono<ProductDetailsResponse> updateProduct(@PathVariable("productId") UUID productId,
                                                      @Valid @RequestBody ProductUpsertRequest request) {
        return productAdminService.updateProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable("productId") UUID productId) {
        return productAdminService.deleteProduct(productId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/{productId}/variants")
    public Flux<VariantResponse> listVariants(@PathVariable("productId") UUID productId) {
        return productAdminService.listVariants(productId);
    }

    @PostMapping("/{productId}/variants")
    public Mono<ResponseEntity<VariantResponse>> createVariant(@PathVariable("productId") UUID productId,
                                                               @Valid @RequestBody VariantUpsertRequest request) {
        return productAdminService.createVariant(productId, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public Mono<VariantResponse> updateVariant(@PathVariable("productId") UUID productId,
                                               @PathVariable("variantId") UUID variantId,
                                               @Valid @RequestBody VariantUpsertRequest request) {
        return productAdminService.updateVariant(productId, variantId, request);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public Mono<ResponseEntity<Void>> deleteVariant(@PathVariable("productId") UUID productId,
                                                    @PathVariable("variantId") UUID variantId) {
        return productAdminService.deleteVariant(productId, variantId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/{productId}/images")
    public Flux<ProductImageResponse> listImages(@PathVariable("productId") UUID productId) {
        return productAdminService.listImages(productId);
    }

    @PostMapping("/{productId}/images")
    public Mono<ResponseEntity<ProductImageResponse>> createImage(@PathVariable("productId") UUID productId,
                                                                  @Valid @RequestBody ProductImageUpsertRequest request) {
        return productAdminService.createImage(productId, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{productId}/images/{imageId}")
    public Mono<ProductImageResponse> updateImage(@PathVariable("productId") UUID productId,
                                                  @PathVariable("imageId") UUID imageId,
                                                  @Valid @RequestBody ProductImageUpsertRequest request) {
        return productAdminService.updateImage(productId, imageId, request);
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public Mono<ResponseEntity<Void>> deleteImage(@PathVariable("productId") UUID productId,
                                                  @PathVariable("imageId") UUID imageId) {
        return productAdminService.deleteImage(productId, imageId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/{productId}/specifications")
    public Flux<SpecificationResponse> listSpecifications(@PathVariable("productId") UUID productId) {
        return productAdminService.listSpecifications(productId);
    }

    @PostMapping("/{productId}/specifications")
    public Mono<ResponseEntity<SpecificationResponse>> createSpecification(@PathVariable("productId") UUID productId,
                                                                           @Valid @RequestBody SpecificationUpsertRequest request) {
        return productAdminService.createSpecification(productId, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{productId}/specifications/{specificationId}")
    public Mono<SpecificationResponse> updateSpecification(@PathVariable("productId") UUID productId,
                                                           @PathVariable("specificationId") UUID specificationId,
                                                           @Valid @RequestBody SpecificationUpsertRequest request) {
        return productAdminService.updateSpecification(productId, specificationId, request);
    }

    @DeleteMapping("/{productId}/specifications/{specificationId}")
    public Mono<ResponseEntity<Void>> deleteSpecification(@PathVariable("productId") UUID productId,
                                                          @PathVariable("specificationId") UUID specificationId) {
        return productAdminService.deleteSpecification(productId, specificationId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/{productId}/reviews")
    public Flux<ReviewResponse> listReviews(@PathVariable("productId") UUID productId) {
        return productAdminService.listReviews(productId);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public Mono<ResponseEntity<Void>> deleteReview(@PathVariable("reviewId") UUID reviewId) {
        return productAdminService.deleteReview(reviewId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
