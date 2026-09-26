package org.example.productservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Table("product_variants")
public class VariantEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("product_id")
    private UUID productId;
    private String name;
    private String sku;
    private BigDecimal price;
    @Column("old_price")
    private BigDecimal oldPrice;
    @Column("stock_quantity")
    private Integer stockQuantity;
    @Column("attributes_json")
    private String attributesJson;
    private boolean active;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;
    @Transient
    private boolean isNew = true;

    public VariantEntity() {
    }

    public VariantEntity(UUID id,
                         UUID productId,
                         String name,
                         String sku,
                         BigDecimal price,
                         BigDecimal oldPrice,
                         Integer stockQuantity,
                         String attributesJson,
                         boolean active,
                         Instant createdAt,
                         Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.oldPrice = oldPrice;
        this.stockQuantity = stockQuantity;
        this.attributesJson = attributesJson;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(BigDecimal oldPrice) {
        this.oldPrice = oldPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public VariantEntity markPersisted() {
        this.isNew = false;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VariantEntity that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

