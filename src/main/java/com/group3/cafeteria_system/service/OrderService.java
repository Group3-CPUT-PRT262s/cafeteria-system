package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.*;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import com.group3.cafeteria_system.repository.OrderItemRepository;
import com.group3.cafeteria_system.repository.OrderRepository;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    // ── Place an order ────────────────────────────
    // @Transactional means: if anything fails, the whole
    // operation is rolled back. Nothing gets half-saved.
    @Transactional
    public Order placeOrder(String username, Long timeSlotId,
                            Map<Long, Integer> cart) {

        // Validate cart is not empty
        if (cart == null || cart.isEmpty()) {
            throw new RuntimeException("Cannot place an empty order.");
        }

        // Validate time slot exists and is active
        TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() ->
                        new RuntimeException("Time slot not found."));

        if (!slot.getIsActive()) {
            throw new RuntimeException("Selected time slot is no longer available.");
        }

        // Calculate total and build order items
        double total = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long itemId = entry.getKey();
            Integer quantity = entry.getValue();

            MenuItem menuItem = menuItemRepository.findById(itemId)
                    .orElseThrow(() ->
                            new RuntimeException("Menu item not found: " + itemId));

            // Check item is still available
            if (!menuItem.isAvailable()) {
                throw new RuntimeException(
                        menuItem.getName() + " is no longer available.");
            }

            // Calculate line total
            double lineTotal = menuItem.getPrice() * quantity;
            total += lineTotal;

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(itemId);
            orderItem.setQuantity(quantity);
            orderItem.setItemPrice(menuItem.getPrice());
            orderItems.add(orderItem);
        }

        // Save the order
        Order order = new Order();
        order.setUsername(username);
        order.setTimeSlotId(timeSlotId);
        order.setTotalPrice(total);
        order.setStatus("Pending");
        Order savedOrder = orderRepository.save(order);

        // Save each order item linked to the order
        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
            orderItemRepository.save(item);
        }

        return savedOrder;
    }

    // ── Retrieval methods ─────────────────────────

    // Get a single order by ID (for confirmation page)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    // Get all orders for a user (for history page)
    public List<Order> getOrdersByUser(String username) {
        return orderRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    // Get items belonging to a specific order
    public List<OrderItem> getItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    // Get all orders grouped by time slot (for staff dashboard)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Get orders for a specific time slot
    public List<Order> getOrdersByTimeSlot(Long timeSlotId) {
        return orderRepository.findByTimeSlotId(timeSlotId);
    }

    // ── Staff order management ────────────────────

    // Update order status (Pending → Ready → Collected)
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }

}