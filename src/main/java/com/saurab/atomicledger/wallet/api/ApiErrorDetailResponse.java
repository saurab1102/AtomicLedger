package com.saurab.atomicledger.wallet.api;

public record ApiErrorDetailResponse(
	String field,
	String message
) {
}
