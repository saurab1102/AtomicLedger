package com.saurab.atomicledger.wallet.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReconciliationResponse", description = "Result returned after a reconciliation run.")
public record ReconciliationResponse(
	@Schema(description = "Overall reconciliation status.", example = "PASS")
	String status,
	@Schema(description = "List of failed reconciliation checks. Empty when status is PASS.")
	List<ReconciliationFailedCheckResponse> failedChecks
) {
}
