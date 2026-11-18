package com.group3.cafeteria_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.model.WalletLedgerEntry;
import com.group3.cafeteria_system.repository.UserRepository;
import com.group3.cafeteria_system.repository.WalletLedgerRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletLedgerRepository ledgerRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(userRepository, ledgerRepository);
    }

    @Test
    void initializesExistingAccountsWithZeroCredits() {
        User user = userWithBalance(null);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.updateWalletBalance(1L, 0.0)).thenReturn(1);

        assertEquals(0.0, walletService.getBalance(1L));
        verify(userRepository).updateWalletBalance(1L, 0.0);
    }

    @Test
    void topsUpOnlyWithSupportedDemoAmounts() {
        User user = userWithBalance(100.0);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.updateWalletBalance(1L, 350.0)).thenReturn(1);

        assertEquals(350.0, walletService.topUp(1L, 250.0));
        verify(userRepository).updateWalletBalance(1L, 350.0);
    }

    @Test
    void rejectsUnsupportedTopUpAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(1L, 125.0));
    }

    @Test
    void debitsWalletCredits() {
        User user = userWithBalance(100.0);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.updateWalletBalance(1L, 62.75)).thenReturn(1);

        walletService.debit(1L, 37.25);

        verify(userRepository).updateWalletBalance(1L, 62.75);
    }

    @Test
    void rejectsCheckoutWhenWalletBalanceIsInsufficient() {
        User user = userWithBalance(20.0);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class,
                () -> walletService.debit(1L, 20.01));
    }

    @Test
    void doesNotCreditTheWalletTwiceForTheSameConfirmationKey() {
        String key = "payment-confirmation-1234";
        User user = userWithBalance(100.0);
        WalletLedgerEntry prior = new WalletLedgerEntry(1L, 100.0, 100.0,
                "CREDIT", "CARD", "WALLET-reference", key, "Wallet load");
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(ledgerRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(prior));

        assertEquals(100.0, walletService.loadCredits(1L, 100.0, "CARD", key));

        verify(userRepository, never()).updateWalletBalance(1L, 200.0);
        verify(ledgerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failedWalletPaymentCannotBeCreditedLater() {
        String key = "failed-payment-key-123";
        User user = userWithBalance(0.0);
        WalletLedgerEntry failed = new WalletLedgerEntry(1L, 100.0, 0.0,
                "CREDIT", "BANK", "WALLET-failed", key, "Wallet load declined");
        failed.setTransactionStatus("FAILED");
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(ledgerRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(failed));

        assertThrows(IllegalStateException.class,
                () -> walletService.loadCredits(1L, 100.0, "BANK", key));

        verify(userRepository, never()).updateWalletBalance(1L, 100.0);
    }

    private User userWithBalance(Double balance) {
        User user = new User("student", "password", "STUDENT");
        user.setUserId(1L);
        user.setWalletBalance(balance);
        return user;
    }
}
