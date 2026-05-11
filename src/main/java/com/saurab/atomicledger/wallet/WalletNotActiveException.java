package com.saurab.atomicledger.wallet;

import java.util.UUID;

public class WalletNotActiveException extends RuntimeException {

	private final UUID walletId;

	public WalletNotActiveException(UUID walletId) {
		super("wallet is not active");
		this.walletId = walletId;
	}

	public UUID getWalletId() {
		return this.walletId;
	}
}
