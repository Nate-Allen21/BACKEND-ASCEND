package br.com.ascend.the_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vault_transactions")
public class VaultTransaction {

    public enum VaultTransactionType {
        INCOME,
        EXPENSE,
        REWARD,
        COSMETIC_UNLOCK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VaultTransactionType type;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected VaultTransaction() {
    }

    public VaultTransaction(User user, VaultTransactionType type, String description, int amount) {
        this.user = user;
        this.type = type;
        this.description = description;
        this.amount = amount;
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public VaultTransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public int getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
