package com.saurab.atomicledger.wallet;

public class SameWalletTransferException extends RuntimeException {

	public SameWalletTransferException() {
		super("source and destination wallets must be different");
	}
}
