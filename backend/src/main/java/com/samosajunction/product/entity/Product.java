package com.samosajunction.product.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "price_paise", nullable = false)
    private int pricePaise;

    @Column(nullable = false)
    private int calories;

    @Column(name = "protein_grams", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinGrams;

    @Enumerated(EnumType.STRING)
    @Column(name = "spice_level", nullable = false, length = 16)
    private SpiceLevel spiceLevel;

    @Column(nullable = false)
    private boolean available = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "product_ingredients", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "ingredient", nullable = false)
    @BatchSize(size = 16)
    private Set<String> ingredients = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "product_dietary_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", nullable = false)
    @BatchSize(size = 16)
    private Set<String> dietaryTags = new HashSet<>();

    protected Product() {
    }

    public Product(
            String name,
            String description,
            Category category,
            int pricePaise,
            int calories,
            BigDecimal proteinGrams,
            SpiceLevel spiceLevel,
            boolean available,
            Set<String> ingredients,
            Set<String> dietaryTags
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.pricePaise = pricePaise;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.spiceLevel = spiceLevel;
        this.available = available;
        this.ingredients = new HashSet<>(ingredients);
        this.dietaryTags = new HashSet<>(dietaryTags);
    }

    public void update(
            String name,
            String description,
            Category category,
            int pricePaise,
            int calories,
            BigDecimal proteinGrams,
            SpiceLevel spiceLevel,
            boolean available,
            Set<String> ingredients,
            Set<String> dietaryTags
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.pricePaise = pricePaise;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.spiceLevel = spiceLevel;
        this.available = available;
        this.ingredients.clear();
        this.ingredients.addAll(ingredients);
        this.dietaryTags.clear();
        this.dietaryTags.addAll(dietaryTags);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getPricePaise() {
        return pricePaise;
    }

    public int getCalories() {
        return calories;
    }

    public BigDecimal getProteinGrams() {
        return proteinGrams;
    }

    public SpiceLevel getSpiceLevel() {
        return spiceLevel;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<String> getIngredients() {
        return ingredients;
    }

    public Set<String> getDietaryTags() {
        return dietaryTags;
    }
}
