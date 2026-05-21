package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TransferResponse", description = "Result returned after a successful transfer.")
public record TransferResponse(
	@Schema(description = "Created transfer transaction identifier.", example = "55555555-5555-5555-5555-555555555555")
	UUID transactionId,
	@Schema(description = "Source wallet identifier.", example = "11111111-1111-1111-1111-111111111111")
	UUID sourceWalletId,
	@Schema(description = "Destination wallet identifier.", example = "22222222-2222-2222-2222-222222222222")
	UUID destinationWalletId,
	@Schema(description = "Transfer amount.", example = "75.00")
	BigDecimal amount,
	@Schema(description = "Transfer currency.", example = "INR")
	String currency,
	@Schema(description = "Transaction type.", example = "TRANSFER")
	String transactionType,
	@Schema(description = "Transaction status.", example = "SUCCEEDED")
	String transactionStatus,
	@Schema(description = "Source wallet available balance after the transfer.", example = "125.00")
	BigDecimal sourceAvailableBalance,
	@Schema(description = "Destination wallet available balance after the transfer.", example = "75.00")
	BigDecimal destinationAvailableBalance
) {
}
