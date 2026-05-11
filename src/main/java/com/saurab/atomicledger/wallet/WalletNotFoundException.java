package com.saurab.atomicledger.wallet;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

	private final String field;
	private final UUID walletId;

	public WalletNotFoundException(UUID walletId) {
		this("walletId", walletId);
	}

	public WalletNotFoundException(String field, UUID walletId) {
		super("wallet not found");
		this.field = field;
		this.walletId = walletId;
	}

	public String getField() {
		return this.field;
	}

	public UUID getWalletId() {
		return this.walletId;
	}
}
