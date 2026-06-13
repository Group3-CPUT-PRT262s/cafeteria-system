package com.group3.cafeteria_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private Integer quantity = 1;

    // Price at time of ordering — preserved even if the menu price changes later
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    // Constructors
    public OrderItem() {}

    public OrderItem(Long orderId, Long menuItemId, Integer quantity, Double unitPrice) {
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    //  Business logic methods

    // Computed — not stored in the database
    // Prevents stale data if quantity or price changes
    public Double getSubtotal() {
        return unitPrice * quantity;
    }

    // Getters and Setters
    public Long getOrderItemId() { return orderItemId; }
    public Long getOrderId() { return orderId; }
    public Long getMenuItemId() { return menuItemId; }
    public Integer getQuantity() { return quantity; }
    public Double getUnitPrice() { return unitPrice; }

    public void setOrderItemId(Long orderItemId) {this.orderItemId = orderItemId;}
    public void setOrderId(Long orderId) {this.orderId = orderId;}
    public void setMenuItemId(Long menuItemId) {this.menuItemId = menuItemId;}
    public void setQuantity(Integer quantity) {this.quantity = quantity;}
    public void setUnitPrice(Double unitPrice) {this.unitPrice = unitPrice;}
}