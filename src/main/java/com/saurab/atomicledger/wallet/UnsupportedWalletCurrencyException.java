package com.saurab.atomicledger.wallet;

public class UnsupportedWalletCurrencyException extends RuntimeException {

	private final String currency;

	public UnsupportedWalletCurrencyException(String currency) {
		super("currency is unsupported");
		this.currency = currency;
	}

	public String getCurrency() {
		return this.currency;
	}
}
