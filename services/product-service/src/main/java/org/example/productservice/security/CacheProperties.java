package org.example.productservice.security;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    @NotNull
    private Duration productTtl = Duration.ofMinutes(10);

    @NotNull
    private Duration categoryTtl = Duration.ofMinutes(30);

    public Duration getProductTtl() {
        return productTtl;
    }

    public void setProductTtl(Duration productTtl) {
        this.productTtl = productTtl;
    }

    public Duration getCategoryTtl() {
        return categoryTtl;
    }

    public void setCategoryTtl(Duration categoryTtl) {
        this.categoryTtl = categoryTtl;
    }
}

