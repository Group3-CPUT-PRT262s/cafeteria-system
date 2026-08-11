package com.group3.cafeteria_system.controller;

import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.service.MenuService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class CartApiController {

    private final MenuService menuService;

    public CartApiController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ── Helper ────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCarts(HttpSession session) { // check later, added "s" to remove ambiguity with other similar method
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private int totalCartItems(Map<Long, Integer> cart) {
        return cart.values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    // ─────────────────────────────────────────
    // GET /api/cart
    // ─────────────────────────────────────────
    // Returns current cart with full item details.
    // Reads item details from the database for
    // each item ID stored in the session cart.
    //
    // DATABASE OPERATION: READ
    // For each item in the session cart, queries
    // menu_items by primary key to get current
    // name, price, and status.
    //
    // Postman: GET /api/cart
    // ─────────────────────────────────────────
    @GetMapping("/api/cart")
    public ResponseEntity<Map<String, Object>> getCart(
            HttpSession session) {

        Map<Long, Integer> cart = getCarts(session);
        List<Map<String, Object>> cartItems = new ArrayList<>();
        double total = 0.0;

        for (Map.Entry<Long, Integer> entry
                : cart.entrySet()) {

            // READ — fetch current item details from DB
            Optional<MenuItem> optItem =
                    menuService.getItemById(entry.getKey());

            if (optItem.isPresent()) {
                MenuItem item    = optItem.get();
                int      qty     = entry.getValue();
                double   subtotal = item.getPrice() * qty;
                total += subtotal;

                Map<String, Object> line = new LinkedHashMap<>();
                line.put("menuItemId",  item.getMenuItemId());
                line.put("itemName",    item.getItemName());
                line.put("price",       item.getPrice());
                line.put("status",      item.getStatus());
                line.put("quantity",    qty);
                line.put("subtotal",    subtotal);
                cartItems.add(line);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items",      cartItems);
        response.put("itemCount",  totalCartItems(cart));
        response.put("totalPrice", total);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // POST /api/cart/items
    // ─────────────────────────────────────────
    // Adds an item to the session cart.
    // Validates against the database that the
    // item exists and is available before adding.
    //
    // DATABASE OPERATION: READ (validation)
    // Reads from menu_items to confirm item
    // exists and status != Sold Out before
    // allowing it into the cart.
    //
    // Body: { "menuItemId": 1, "quantity": 2 }
    // Postman: POST /api/cart/items
    // ─────────────────────────────────────────
    @PostMapping("/api/cart/items")
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        Long menuItemId = Long.valueOf(
                body.get("menuItemId").toString());
        int quantity = body.containsKey("quantity")
                ? Integer.parseInt(body.get("quantity").toString())
                : 1;

        if (quantity < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Quantity must be at least 1."));
        }

        // READ — validate item exists and is available
        Optional<MenuItem> optItem =
                menuService.getItemById(menuItemId);

        if (optItem.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Menu item not found."));
        }

        MenuItem item = optItem.get();
        if (!item.isAvailable()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", item.getItemName() + " is currently unavailable."));
        }

        // Add or increment in session cart
        Map<Long, Integer> cart = getCarts(session);
        cart.merge(menuItemId, quantity, Integer::sum);
        session.setAttribute("cart", cart);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",    "success");
        response.put("message",   item.getItemName() + " added to cart.");
        response.put("itemName",  item.getItemName());
        response.put("quantity",  cart.get(menuItemId));
        response.put("cartCount", totalCartItems(cart));
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // PUT /api/cart/items/{menuItemId}
    // ─────────────────────────────────────────
    // Updates the quantity of a specific item
    // already in the cart.
    //
    // Body: { "quantity": 3 }
    // Postman: PUT /api/cart/items/1
    // ─────────────────────────────────────────
    @PutMapping("/api/cart/items/{menuItemId}")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable Long menuItemId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        int quantity = Integer.parseInt(
                body.get("quantity").toString());

        if (quantity < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                            "message",
                            "Quantity must be at least 1. " +
                                    "Use DELETE to remove an item."));
        }

        Map<Long, Integer> cart = getCarts(session);
        if (!cart.containsKey(menuItemId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "Item not found in cart."));
        }

        cart.put(menuItemId, quantity);
        session.setAttribute("cart", cart);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",     "success");
        response.put("message",    "Cart updated.");
        response.put("menuItemId", menuItemId);
        response.put("quantity",   quantity);
        response.put("cartCount",  totalCartItems(cart));
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // DELETE /api/cart/items/{menuItemId}
    // ─────────────────────────────────────────
    // Removes a single item from the cart.
    //
    // Postman: DELETE /api/cart/items/1
    // ─────────────────────────────────────────
    @DeleteMapping("/api/cart/items/{menuItemId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable Long menuItemId,
            HttpSession session) {

        Map<Long, Integer> cart = getCarts(session);
        if (!cart.containsKey(menuItemId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "Item not found in cart."));
        }

        cart.remove(menuItemId);
        session.setAttribute("cart", cart);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",    "success");
        response.put("message",   "Item removed from cart.");
        response.put("cartCount", totalCartItems(cart));
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // DELETE /api/cart
    // ─────────────────────────────────────────
    // Clears the entire cart.
    //
    // Postman: DELETE /api/cart
    // ─────────────────────────────────────────
    @DeleteMapping("/api/cart")
    public ResponseEntity<Map<String, Object>> clearCart(
            HttpSession session) {

        session.removeAttribute("cart");
        return ResponseEntity.ok(Map.of(
                "status",    "success",
                "message",   "Cart cleared.",
                "cartCount", 0
        ));
    }
}