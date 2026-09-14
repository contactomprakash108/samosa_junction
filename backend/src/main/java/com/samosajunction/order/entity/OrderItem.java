package com.samosajunction.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "unit_price_paise", nullable = false)
    private int unitPricePaise;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    public OrderItem(UUID productId, String productName, int unitPricePaise, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPricePaise = unitPricePaise;
        this.quantity = quantity;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public int lineTotalPaise() {
        return unitPricePaise * quantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getUnitPricePaise() {
        return unitPricePaise;
    }

    public int getQuantity() {
        return quantity;
    }
}
