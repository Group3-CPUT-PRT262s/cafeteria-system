package com.group3.cafeteria_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which order this line item belongs to.
    // Stored as a plain Long to keep things simple
    // for the demo phase.
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // Which menu item was ordered.
    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    // How many of this item were ordered.
    @Column(nullable = false)
    private Integer quantity = 1;

    // Price at the time of ordering.
    // Stored separately from MenuItem.price because
    // the menu price might change later — this preserves
    // the accurate historical record for this order.
    @Column(name = "item_price", nullable = false)
    private Double itemPrice;

    // ── Constructors ──────────────────────────────
    public OrderItem() {}

    public OrderItem(Long orderId, Long menuItemId,
                     Integer quantity, Double itemPrice) {
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.itemPrice = itemPrice;
    }

    // ── Business logic methods ────────────────────

    // Calculates the total cost for this line item.
    // e.g. 2 x Chicken Wrap at R45 = R90
    public Double getSubtotal() {
        return itemPrice * quantity;
    }

    // ── Getters and Setters ───────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getItemPrice() { return itemPrice; }
    public void setItemPrice(Double itemPrice) {
        this.itemPrice = itemPrice;
    }
}