package com.group3.cafeteria_system.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet_ledger")
public class WalletLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "balance_after", nullable = false)
    private Double balanceAfter;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String description;

    @Column(name = "transaction_status", nullable = false)
    private String transactionStatus = "COMPLETED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public WalletLedgerEntry() {}

    public WalletLedgerEntry(Long userId, Double amount, Double balanceAfter,
                             String transactionType, String paymentMethod,
                             String reference, String idempotencyKey, String description) {
        this.userId = userId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.transactionType = transactionType;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.transactionStatus = "COMPLETED";
    }

    public Long getTransactionId() { return transactionId; }
    public Long getUserId() { return userId; }
    public Double getAmount() { return amount; }
    public Double getBalanceAfter() { return balanceAfter; }
    public String getTransactionType() { return transactionType; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getReference() { return reference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getDescription() { return description; }
    public String getTransactionStatus() { return transactionStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
}
