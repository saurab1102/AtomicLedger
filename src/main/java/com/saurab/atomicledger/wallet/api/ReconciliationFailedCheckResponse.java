package com.saurab.atomicledger.wallet.api;

public record ReconciliationFailedCheckResponse(
	String checkType,
	String entityType,
	String entityId,
	String message
) {
}
