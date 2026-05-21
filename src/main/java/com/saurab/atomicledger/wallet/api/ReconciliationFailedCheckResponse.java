package com.saurab.atomicledger.wallet.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReconciliationFailedCheckResponse", description = "Single reconciliation check failure.")
public record ReconciliationFailedCheckResponse(
	@Schema(description = "Failure type identifier.", example = "WALLET_BALANCE_MISMATCH")
	String checkType,
	@Schema(description = "Entity type associated with the failed check.", example = "WALLET")
	String entityType,
	@Schema(description = "Identifier of the entity associated with the failed check.", example = "11111111-1111-1111-1111-111111111111")
	String entityId,
	@Schema(description = "Human-readable reconciliation failure message.", example = "wallet balance does not match ledger total")
	String message
) {
}
