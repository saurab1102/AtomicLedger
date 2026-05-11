package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositWalletRequest(
	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.01", message = "amount must be positive")
	BigDecimal amount,
	@NotBlank(message = "currency is required")
	String currency
) {
}
