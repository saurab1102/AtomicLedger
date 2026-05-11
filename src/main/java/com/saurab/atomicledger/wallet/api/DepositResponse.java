package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositResponse(
	UUID transactionId,
	UUID walletId,
	BigDecimal amount,
	String currency,
	String transactionType,
	String transactionStatus,
	BigDecimal availableBalance
) {
}
