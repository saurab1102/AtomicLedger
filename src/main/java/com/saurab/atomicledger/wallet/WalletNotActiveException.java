package com.saurab.atomicledger.wallet;

import java.util.UUID;

public class WalletNotActiveException extends RuntimeException {

	private final String field;
	private final UUID walletId;

	public WalletNotActiveException(UUID walletId) {
		this("walletId", walletId);
	}

	public WalletNotActiveException(String field, UUID walletId) {
		super("wallet is not active");
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
