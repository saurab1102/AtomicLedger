package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallets")
public class Wallet {

	@Id
	private UUID id;

	@Column(name = "owner_reference", nullable = false)
	private String ownerReference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private WalletCurrency currency;

	@Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal availableBalance;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private WalletStatus status;

	protected Wallet() {
	}

	public Wallet(UUID id, String ownerReference, WalletCurrency currency, BigDecimal availableBalance, WalletStatus status) {
		this.id = id;
		this.ownerReference = ownerReference;
		this.currency = currency;
		this.availableBalance = availableBalance;
		this.status = status;
	}

	public UUID getId() {
		return this.id;
	}

	public String getOwnerReference() {
		return this.ownerReference;
	}

	public WalletCurrency getCurrency() {
		return this.currency;
	}

	public BigDecimal getAvailableBalance() {
		return this.availableBalance;
	}

	public WalletStatus getStatus() {
		return this.status;
	}

	public boolean isActive() {
		return this.status == WalletStatus.ACTIVE;
	}

	public void credit(BigDecimal amount) {
		this.availableBalance = this.availableBalance.add(amount);
	}
}
