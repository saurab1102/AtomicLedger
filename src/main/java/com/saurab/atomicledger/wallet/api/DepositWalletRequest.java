package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "DepositWalletRequest", description = "Request body for depositing funds into a wallet.")
public record DepositWalletRequest(
	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.01", message = "amount must be positive")
	@Schema(description = "Deposit amount.", example = "125.50")
	BigDecimal amount,
	@NotBlank(message = "currency is required")
	@Schema(description = "Deposit currency.", example = "INR")
	String currency
) {
}
