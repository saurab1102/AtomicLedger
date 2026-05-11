package com.saurab.atomicledger.wallet;

import java.util.Locale;

public enum WalletCurrency {
	INR;

	public static WalletCurrency from(String currency) {
		String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

		if (!INR.name().equals(normalizedCurrency)) {
			throw new UnsupportedWalletCurrencyException(currency);
		}
		return INR;
	}
}
