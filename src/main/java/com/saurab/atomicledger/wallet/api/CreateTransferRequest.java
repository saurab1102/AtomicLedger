package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CreateTransferRequest", description = "Request body for a wallet-to-wallet transfer.")
public record CreateTransferRequest(
	@NotNull(message = "sourceWalletId is required")
	@Schema(description = "Source wallet identifier.", example = "11111111-1111-1111-1111-111111111111")
	UUID sourceWalletId,
	@NotNull(message = "destinationWalletId is required")
	@Schema(description = "Destination wallet identifier.", example = "22222222-2222-2222-2222-222222222222")
	UUID destinationWalletId,
	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.01", message = "amount must be positive")
	@Schema(description = "Transfer amount.", example = "75.00")
	BigDecimal amount,
	@NotBlank(message = "currency is required")
	@Schema(description = "Transfer currency.", example = "INR")
	String currency
) {
}
