package com.group3.cafeteria_system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.model.OrderItem;
import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.repository.CustomerOrderRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import com.group3.cafeteria_system.repository.OrderItemRepository;
import com.group3.cafeteria_system.repository.TimeSlotRepository;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final InventoryService inventoryService;
    private final WalletService walletService;

    public OrderService(CustomerOrderRepository customerOrderRepository,
                        OrderItemRepository orderItemRepository,
                        MenuItemRepository menuItemRepository,
                        TimeSlotRepository timeSlotRepository,
                        InventoryService inventoryService,
                        WalletService walletService) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.inventoryService = inventoryService;
        this.walletService = walletService;
    }

    // Place an order
    // Note: I've replaced the username parameter with userId (Long)
    @Transactional
    public CustomerOrder placeOrder(Long userId, Long timeSlotId,
                                    Map<Long, Integer> cart) {

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an empty order."); // null checker exception
        }

        if (timeSlotId == null || userId == null) {
            throw new IllegalArgumentException("A customer and collection time are required.");
        }

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(timeSlotId)
                .orElseThrow(() ->
                        new RuntimeException("Time slot not found."));

        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw new IllegalStateException("Selected time slot is no longer available.");
        }

        long activeOrders = customerOrderRepository.countActiveOrdersByTimeSlotId(timeSlotId);
        if (slot.getMaxOrders() == null || activeOrders >= slot.getMaxOrders()) {
            throw new IllegalStateException("This collection time is fully booked. Please choose another.");
        }

        double total = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();

        if (cart.keySet().stream().anyMatch(itemId -> itemId == null)) {
            throw new IllegalArgumentException("Every order item must be selected from the menu.");
        }
        for (Long itemId : cart.keySet().stream().sorted().toList()) {
            Integer quantity = cart.get(itemId);

            if (itemId == null || quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Every order quantity must be at least one.");
            }
            MenuItem menuItem = menuItemRepository.findById(itemId)
                    .orElseThrow(() ->
                            new RuntimeException("Menu item not found: " + itemId));

            if (!Boolean.TRUE.equals(menuItem.getIsActive())
                    || "Sold Out".equals(menuItem.getStatus())) {
                throw new IllegalStateException(
                        menuItem.getItemName() + " is no longer available.");
            }
            int maxQuantity = menuItem.getMaxOrderQuantity() == null
                    ? 10 : menuItem.getMaxOrderQuantity();
            if (quantity > maxQuantity) {
                throw new IllegalArgumentException(menuItem.getItemName()
                        + " has a maximum order quantity of " + maxQuantity + ".");
            }
            if (menuItem.getPrice() == null || !Double.isFinite(menuItem.getPrice())
                    || menuItem.getPrice() <= 0) {
                throw new IllegalStateException(menuItem.getItemName() + " has an invalid price.");
            }

            double lineTotal = menuItem.getPrice() * quantity;
            if (!Double.isFinite(lineTotal)) {
                throw new IllegalArgumentException("The order total is too large to process.");
            }
            total += lineTotal;
            if (!Double.isFinite(total)) {
                throw new IllegalArgumentException("The order total is too large to process.");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(itemId);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItems.add(orderItem);
        }
        total = Math.round(total * 100.0) / 100.0;
        if (!Double.isFinite(total) || total <= 0) {
            throw new IllegalArgumentException("The order total must be greater than zero.");
        }

        CustomerOrder order = new CustomerOrder();
        order.setUserId(userId);
        order.setTimeSlotId(timeSlotId);
        order.setTotalAmount(total);
        order.setOrderStatus("Pending");
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getOrderId());
            orderItemRepository.save(item);
            inventoryService.reserveStock(item.getMenuItemId(), item.getQuantity());
        }

        return savedOrder;
    }

    // Retrieval methods

    public Optional<CustomerOrder> getOrderById(Long id) {
        return customerOrderRepository.findById(id);
    }

    // Now takes userId (Long) instead of username (String)
    public List<CustomerOrder> getOrdersByUser(Long userId) {
        return customerOrderRepository.findByUserIdOrderByOrderedAtDesc(userId);
    }

    public List<OrderItem> getItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public List<CustomerOrder> getAllOrders() {
        return customerOrderRepository.findAll();
    }

    public List<CustomerOrder> getOrdersByTimeSlot(Long timeSlotId) {
        return customerOrderRepository.findByTimeSlotId(timeSlotId);
    }

    // Staff order management

    @Transactional
    public CustomerOrder updateOrderStatus(Long orderId, String status) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId));

        String currentStatus = canonicalStatus(order.getOrderStatus());
        boolean validTransition = switch (currentStatus) {
            case "Pending" -> "Prepared".equals(status) || "Cancelled".equals(status);
            case "Prepared" -> "Ready".equals(status) || "Cancelled".equals(status);
            case "Ready" -> "Collected".equals(status) || "Cancelled".equals(status);
            default -> false;
        };
        if (!validTransition) {
            throw new IllegalStateException(
                    "Cannot change order status from " + order.getOrderStatus() + " to " + status + ".");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if ("Cancelled".equals(status)) {
            for (OrderItem item : items) {
                inventoryService.releaseStock(item.getMenuItemId(), item.getQuantity());
            }
            walletService.refundOrder(order.getUserId(), orderId, order.getTotalAmount());
        } else if ("Collected".equals(status)) {
            for (OrderItem item : items) {
                inventoryService.commitReservedStock(item.getMenuItemId(), item.getQuantity());
            }
        }

        order.setOrderStatus(status);
        return customerOrderRepository.save(order);
    }

    @Transactional
    public MenuItem markOrderItemOutOfStock(Long orderId, Long menuItemId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        if ("Collected".equals(canonicalStatus(order.getOrderStatus()))
                || "Cancelled".equals(canonicalStatus(order.getOrderStatus()))) {
            throw new IllegalStateException("Items on completed or cancelled orders cannot be changed.");
        }

        boolean belongsToOrder = orderItemRepository.findByOrderId(orderId).stream()
                .anyMatch(item -> item.getMenuItemId().equals(menuItemId));
        if (!belongsToOrder) {
            throw new IllegalArgumentException("The selected item is not part of this order.");
        }

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + menuItemId));
        menuItem.setStatus("Sold Out");
        return menuItemRepository.save(menuItem);
    }

    public CustomerOrder advanceOrderStatus(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId));

        String nextStatus = switch (canonicalStatus(order.getOrderStatus())) {
            case "Pending" -> "Prepared";
            case "Prepared" -> "Ready";
            case "Ready" -> "Collected";
            default -> throw new IllegalStateException(
                    "Order status cannot advance from " + order.getOrderStatus() + ".");
        };
        return updateOrderStatus(orderId, nextStatus);
    }

    public List<CustomerOrder> getAllOrdersOldestFirst() {
        return customerOrderRepository.findAllByOrderByOrderedAtAsc();
    }

    private String canonicalStatus(String status) {
        return switch (status == null ? "" : status) {
            case "Confirmed" -> "Pending";
            case "Preparing" -> "Prepared";
            default -> status;
        };
    }
}