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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false, unique = true)
	private WalletTransaction transaction;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "wallet_id", nullable = false)
	private Wallet wallet;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 32)
	private LedgerEntryType entryType;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private WalletCurrency currency;

	protected LedgerEntry() {
	}

	public LedgerEntry(
		UUID id,
		WalletTransaction transaction,
		Wallet wallet,
		LedgerEntryType entryType,
		BigDecimal amount,
		WalletCurrency currency
	) {
		this.id = id;
		this.transaction = transaction;
		this.wallet = wallet;
		this.entryType = entryType;
		this.amount = amount;
		this.currency = currency;
	}

	public UUID getId() {
		return this.id;
	}

	public WalletTransaction getTransaction() {
		return this.transaction;
	}

	public Wallet getWallet() {
		return this.wallet;
	}

	public LedgerEntryType getEntryType() {
		return this.entryType;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public WalletCurrency getCurrency() {
		return this.currency;
	}
}
