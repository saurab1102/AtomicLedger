package com.saurab.atomicledger.wallet.api;

import java.util.List;

public record ReconciliationResponse(
	String status,
	List<ReconciliationFailedCheckResponse> failedChecks
) {
}
