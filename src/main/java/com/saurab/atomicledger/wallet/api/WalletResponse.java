package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletResponse", description = "Wallet representation returned by the API.")
public record WalletResponse(
	@Schema(description = "Wallet identifier.", example = "11111111-1111-1111-1111-111111111111")
	UUID id,
	@Schema(description = "Client-owned reference for the wallet owner.", example = "customer-123")
	String ownerReference,
	@Schema(description = "Wallet currency.", example = "INR")
	String currency,
	@Schema(description = "Wallet available balance.", example = "0.00")
	BigDecimal availableBalance,
	@Schema(description = "Wallet status.", example = "ACTIVE")
	String status
) {
}
