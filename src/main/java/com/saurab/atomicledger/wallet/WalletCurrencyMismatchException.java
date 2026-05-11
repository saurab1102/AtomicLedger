package com.saurab.atomicledger.wallet;

public class WalletCurrencyMismatchException extends RuntimeException {

	public WalletCurrencyMismatchException() {
		super("currency must match both wallets");
	}
}
