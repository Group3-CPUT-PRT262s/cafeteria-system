package com.group3.cafeteria_system.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.model.WalletLedgerEntry;
import com.group3.cafeteria_system.repository.UserRepository;
import com.group3.cafeteria_system.repository.WalletLedgerRepository;

@Service
public class WalletService {

    public static final List<Double> DEMO_TOP_UP_AMOUNTS = List.of(100.0, 250.0, 500.0);
    private static final double STARTING_BALANCE = 0.0;

    private final UserRepository userRepository;
    private final WalletLedgerRepository ledgerRepository;

    public WalletService(UserRepository userRepository,
                         WalletLedgerRepository ledgerRepository) {
        this.userRepository = userRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public double getBalance(Long userId) {
        User user = getLockedUser(userId);
        Double balance = user.getWalletBalance();
        if (balance == null) {
            balance = STARTING_BALANCE;
            updateBalance(userId, balance);
        }
        return balance;
    }

    @Transactional
    public double topUp(Long userId, double amount) {
        return loadCredits(userId, amount, "CARD", java.util.UUID.randomUUID().toString());
    }

    @Transactional
    public void initializeWallet(Long userId) {
        User user = getLockedUser(userId);
        String key = "wallet-opening-" + userId;
        if (ledgerRepository.findByIdempotencyKey(key).isPresent()) {
            return;
        }
        if (user.getWalletBalance() == null) {
            updateBalance(userId, STARTING_BALANCE);
        }
        ledgerRepository.save(new WalletLedgerEntry(
                userId, 0.0, user.getWalletBalance() == null
                        ? STARTING_BALANCE : user.getWalletBalance(),
                "OPENING", "SYSTEM", "WALLET-OPENING-" + userId, key,
                "Opening wallet balance"));
    }

    @Transactional
    public double loadCredits(Long userId, double amount, String paymentMethod,
                              String idempotencyKey) {
        if (!DEMO_TOP_UP_AMOUNTS.contains(amount)) {
            throw new IllegalArgumentException("Choose one of the available demo credit amounts.");
        }
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();
        if (!List.of("CARD", "BANK").contains(method)) {
            throw new IllegalArgumentException("Choose a valid payment method.");
        }
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9-]{16,80}")) {
            throw new IllegalArgumentException("The payment confirmation token is invalid.");
        }

        User user = getLockedUser(userId);
        WalletLedgerEntry priorTransaction = ledgerRepository
                .findByIdempotencyKey(idempotencyKey).orElse(null);
        if (priorTransaction != null) {
            if (!priorTransaction.getUserId().equals(userId)
                    || Math.abs(priorTransaction.getAmount() - amount) > 0.000001
                    || !method.equals(priorTransaction.getPaymentMethod())) {
                throw new IllegalArgumentException("This payment confirmation token was already used.");
            }
            if (!"COMPLETED".equals(priorTransaction.getTransactionStatus())) {
                throw new IllegalStateException("This wallet payment did not complete and cannot be credited.");
            }
            return currentBalance(user, userId);
        }

        double balance = currentBalance(user, userId);
        double updatedBalance = roundToCents(balance + amount);
        updateBalance(userId, updatedBalance);
        ledgerRepository.save(new WalletLedgerEntry(
                userId, amount, updatedBalance, "CREDIT", method,
                "WALLET-" + java.util.UUID.randomUUID(), idempotencyKey,
                "Wallet credit loaded via simulated " + method.toLowerCase() + " verification"));
        return updatedBalance;
    }

    @Transactional
    public void recordTopUpOutcome(Long userId, double amount, String paymentMethod,
                                   String idempotencyKey, String status) {
        if (!DEMO_TOP_UP_AMOUNTS.contains(amount)
                || !List.of("FAILED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("The wallet payment outcome is invalid.");
        }
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();
        if (!List.of("CARD", "BANK").contains(method)
                || idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9-]{16,80}")) {
            throw new IllegalArgumentException("The wallet payment details are invalid.");
        }
        User user = getLockedUser(userId);
        WalletLedgerEntry prior = ledgerRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (prior != null) {
            if (!prior.getUserId().equals(userId)
                    || Math.abs(prior.getAmount() - amount) > 0.000001
                    || !method.equals(prior.getPaymentMethod())) {
                throw new IllegalArgumentException("This payment confirmation token was already used.");
            }
            return;
        }
        WalletLedgerEntry entry = new WalletLedgerEntry(
                userId, amount, currentBalance(user, userId), "CREDIT", method,
                "WALLET-" + java.util.UUID.randomUUID(), idempotencyKey,
                "Wallet load " + status.toLowerCase() + " via simulated " + method.toLowerCase() + " verification");
        entry.setTransactionStatus(status);
        ledgerRepository.save(entry);
    }

    @Transactional
    public void debit(Long userId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("The order total must be greater than zero.");
        }

        User user = getLockedUser(userId);
        double balance = currentBalance(user, userId);
        if (balance + 0.000001 < amount) {
            throw new IllegalStateException(
                    "Insufficient wallet credits. Add demo credits to your wallet and try again.");
        }

        updateBalance(userId, roundToCents(balance - amount));
    }

    @Transactional
    public void chargeOrder(Long userId, Long orderId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("The order total must be greater than zero.");
        }
        User user = getLockedUser(userId);
        double currentBalance = currentBalance(user, userId);
        if (currentBalance + 0.000001 < amount) {
            throw new IllegalStateException(
                    "Insufficient wallet credits. Add demo credits to your wallet and try again.");
        }
        double balance = roundToCents(currentBalance - amount);
        updateBalance(userId, balance);
        String reference = "ORDER-" + orderId;
        ledgerRepository.save(new WalletLedgerEntry(
                userId, -roundToCents(amount), balance, "PURCHASE", "WALLET",
                reference, "order-charge-" + orderId,
                "Wallet payment for order #" + orderId));
    }

    @Transactional
    public void refundOrder(Long userId, Long orderId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("The order refund amount must be greater than zero.");
        }
        String key = "order-refund-" + orderId;
        if (ledgerRepository.findByIdempotencyKey(key).isPresent()) {
            return;
        }
        User user = getLockedUser(userId);
        double balance = roundToCents(currentBalance(user, userId) + amount);
        updateBalance(userId, balance);
        ledgerRepository.save(new WalletLedgerEntry(
                userId, roundToCents(amount), balance, "REFUND", "WALLET",
                "REFUND-ORDER-" + orderId, key,
                "Refund for cancelled order #" + orderId));
    }

    @Transactional(readOnly = true)
    public List<WalletLedgerEntry> getTransactions(Long userId) {
        return ledgerRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private User getLockedUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));
    }

    private double currentBalance(User user, Long userId) {
        Double balance = user.getWalletBalance();
        if (balance != null) {
            return balance;
        }
        updateBalance(userId, STARTING_BALANCE);
        return STARTING_BALANCE;
    }

    private void updateBalance(Long userId, double balance) {
        if (userRepository.updateWalletBalance(userId, balance) != 1) {
            throw new IllegalStateException("Wallet balance could not be updated.");
        }
    }

    private double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
