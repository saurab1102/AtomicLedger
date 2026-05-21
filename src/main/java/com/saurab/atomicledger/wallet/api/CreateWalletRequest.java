package com.saurab.atomicledger.wallet.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateWalletRequest", description = "Request body for creating a wallet.")
public record CreateWalletRequest(
	@NotBlank(message = "ownerReference is required")
	@Schema(description = "Client-owned reference for the wallet owner.", example = "customer-123")
	String ownerReference,
	@NotBlank(message = "currency is required")
	@Schema(description = "Wallet currency.", example = "INR")
	String currency
) {
}
