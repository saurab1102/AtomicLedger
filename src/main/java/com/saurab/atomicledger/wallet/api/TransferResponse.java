package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
	UUID transactionId,
	UUID sourceWalletId,
	UUID destinationWalletId,
	BigDecimal amount,
	String currency,
	String transactionType,
	String transactionStatus,
	BigDecimal sourceAvailableBalance,
	BigDecimal destinationAvailableBalance
) {
}
