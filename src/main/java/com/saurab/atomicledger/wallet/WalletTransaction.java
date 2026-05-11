package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class WalletTransaction {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "wallet_id", nullable = false)
	private Wallet wallet;

	@Column(name = "idempotency_key", nullable = false, unique = true)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 32)
	private WalletTransactionType transactionType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private WalletTransactionStatus status;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private WalletCurrency currency;

	@Column(name = "resulting_available_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal resultingAvailableBalance;

	protected WalletTransaction() {
	}

	public WalletTransaction(
		UUID id,
		Wallet wallet,
		String idempotencyKey,
		WalletTransactionType transactionType,
		WalletTransactionStatus status,
		BigDecimal amount,
		WalletCurrency currency,
		BigDecimal resultingAvailableBalance
	) {
		this.id = id;
		this.wallet = wallet;
		this.idempotencyKey = idempotencyKey;
		this.transactionType = transactionType;
		this.status = status;
		this.amount = amount;
		this.currency = currency;
		this.resultingAvailableBalance = resultingAvailableBalance;
	}

	public UUID getId() {
		return this.id;
	}

	public Wallet getWallet() {
		return this.wallet;
	}

	public String getIdempotencyKey() {
		return this.idempotencyKey;
	}

	public WalletTransactionType getTransactionType() {
		return this.transactionType;
	}

	public WalletTransactionStatus getStatus() {
		return this.status;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public WalletCurrency getCurrency() {
		return this.currency;
	}

	public BigDecimal getResultingAvailableBalance() {
		return this.resultingAvailableBalance;
	}
}
