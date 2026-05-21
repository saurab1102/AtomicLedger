package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletTransactionHistoryItemResponse", description = "Single wallet-relative transaction history item.")
public record WalletTransactionHistoryItemResponse(
	@Schema(description = "Transaction identifier.", example = "55555555-5555-5555-5555-555555555555")
	UUID transactionId,
	@Schema(description = "Transaction type.", example = "TRANSFER")
	String type,
	@Schema(description = "Transaction status.", example = "SUCCEEDED")
	String status,
	@Schema(description = "Wallet-relative direction.", example = "DEBIT")
	String direction,
	@Schema(description = "Transaction amount.", example = "75.00")
	BigDecimal amount,
	@Schema(description = "Transaction currency.", example = "INR")
	String currency,
	@Schema(description = "Counterparty wallet identifier for transfers, if applicable.", example = "22222222-2222-2222-2222-222222222222", nullable = true)
	UUID counterpartyWalletId,
	@Schema(description = "Transaction creation timestamp.", example = "2026-05-21T00:00:00Z")
	Instant createdAt
) {
}
