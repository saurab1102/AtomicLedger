package com.saurab.atomicledger.wallet.api;

public record FieldErrorResponse(
	String field,
	String message
) {
}
