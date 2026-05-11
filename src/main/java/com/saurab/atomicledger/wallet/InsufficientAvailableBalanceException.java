package com.saurab.atomicledger.wallet;

public class InsufficientAvailableBalanceException extends RuntimeException {

	public InsufficientAvailableBalanceException() {
		super("insufficient available balance");
	}
}
