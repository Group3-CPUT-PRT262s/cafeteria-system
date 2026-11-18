package com.group3.cafeteria_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group3.cafeteria_system.model.WalletLedgerEntry;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, Long> {

    Optional<WalletLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    List<WalletLedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
}
