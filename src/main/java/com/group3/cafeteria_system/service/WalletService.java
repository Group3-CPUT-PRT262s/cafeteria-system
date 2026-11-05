package com.group3.cafeteria_system.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.UserRepository;

@Service
public class WalletService {

    public static final List<Double> DEMO_TOP_UP_AMOUNTS = List.of(100.0, 250.0, 500.0);
    private static final double STARTING_BALANCE = 500.0;

    private final UserRepository userRepository;

    public WalletService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        if (!DEMO_TOP_UP_AMOUNTS.contains(amount)) {
            throw new IllegalArgumentException("Choose one of the available demo credit amounts.");
        }

        User user = getLockedUser(userId);
        double balance = currentBalance(user, userId);
        double updatedBalance = roundToCents(balance + amount);
        updateBalance(userId, updatedBalance);
        return updatedBalance;
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
