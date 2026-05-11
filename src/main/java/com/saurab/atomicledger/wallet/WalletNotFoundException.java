package com.saurab.atomicledger.wallet;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

	private final UUID walletId;

	public WalletNotFoundException(UUID walletId) {
		super("wallet not found");
		this.walletId = walletId;
	}

	public UUID getWalletId() {
		return this.walletId;
	}
}
