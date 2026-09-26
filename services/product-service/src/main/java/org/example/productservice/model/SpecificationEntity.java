package org.example.productservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

@Table("product_specifications")
public class SpecificationEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("product_id")
    private UUID productId;
    @Column("label")
    private String label;
    private String value;
    private String unit;
    @Column("display_order")
    private Integer displayOrder;
    @Transient
    private boolean isNew = true;

    public SpecificationEntity() {
    }

    public SpecificationEntity(UUID id, UUID productId, String label, String value, String unit, Integer displayOrder) {
        this.id = id;
        this.productId = productId;
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.displayOrder = displayOrder;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public SpecificationEntity markPersisted() {
        this.isNew = false;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpecificationEntity that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

