package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionHistoryItemResponse(
	UUID transactionId,
	String type,
	String status,
	String direction,
	BigDecimal amount,
	String currency,
	UUID counterpartyWalletId,
	Instant createdAt
) {
}
