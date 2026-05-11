package com.saurab.atomicledger.wallet;

public class MissingIdempotencyKeyException extends RuntimeException {

	public MissingIdempotencyKeyException() {
		super("Idempotency-Key header is required");
	}
}
