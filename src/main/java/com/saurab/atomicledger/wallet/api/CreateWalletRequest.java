package com.saurab.atomicledger.wallet.api;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(
	@NotBlank(message = "ownerReference is required")
	String ownerReference,
	@NotBlank(message = "currency is required")
	String currency
) {
}
