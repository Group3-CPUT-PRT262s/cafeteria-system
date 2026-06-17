package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.*;
import com.group3.cafeteria_system.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TimeSlotRepository timeSlotRepository;

    public OrderService(CustomerOrderRepository customerOrderRepository, OrderItemRepository orderItemRepository, MenuItemRepository menuItemRepository, TimeSlotRepository timeSlotRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    // Place an order
    // Note: I've replaced the username parameter with userId (Long)
    @Transactional
    public CustomerOrder placeOrder(Long userId, Long timeSlotId,
                                    Map<Long, Integer> cart) {

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an empty order."); // null checker exception
        }

        TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() ->
                        new RuntimeException("Time slot not found."));

        if (!slot.getIsActive()) {
            throw new IllegalStateException("Selected time slot is no longer available.");
        }

        // Check the slot hasn't exceeded its max_orders limit
        List<CustomerOrder> existingOrders =
                customerOrderRepository.findByTimeSlotId(timeSlotId);
        if (existingOrders.size() >= slot.getMaxOrders()) {
            throw new IllegalStateException("Cannot place an order with more than " + slot.getMaxOrders() +
                    "This time slot is fully booked. Please choose another.");
        }

        double total = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long itemId = entry.getKey();
            Integer quantity = entry.getValue();

            MenuItem menuItem = menuItemRepository.findById(itemId)
                    .orElseThrow(() ->
                            new RuntimeException("Menu item not found: " + itemId));

            if (!menuItem.isAvailable()) {
                throw new IllegalStateException(
                        menuItem.getItemName() + " is no longer available.");
            }

            double lineTotal = menuItem.getPrice() * quantity;
            total += lineTotal;

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(itemId);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItems.add(orderItem);
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

    public CustomerOrder updateOrderStatus(Long orderId, String status) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId));

        order.setOrderStatus(status);
        return customerOrderRepository.save(order);
    }

    // New — uses the advanceStatus() method on CustomerOrder
    public CustomerOrder advanceOrderStatus(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId));

        order.advanceStatus();
        return customerOrderRepository.save(order);
    }
}