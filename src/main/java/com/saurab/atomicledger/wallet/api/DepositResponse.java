package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DepositResponse", description = "Result returned after a successful deposit.")
public record DepositResponse(
	@Schema(description = "Created deposit transaction identifier.", example = "33333333-3333-3333-3333-333333333333")
	UUID transactionId,
	@Schema(description = "Wallet that received the deposit.", example = "11111111-1111-1111-1111-111111111111")
	UUID walletId,
	@Schema(description = "Deposited amount.", example = "125.50")
	BigDecimal amount,
	@Schema(description = "Deposit currency.", example = "INR")
	String currency,
	@Schema(description = "Transaction type.", example = "DEPOSIT")
	String transactionType,
	@Schema(description = "Transaction status.", example = "SUCCEEDED")
	String transactionStatus,
	@Schema(description = "Wallet available balance after the deposit.", example = "125.50")
	BigDecimal availableBalance
) {
}
