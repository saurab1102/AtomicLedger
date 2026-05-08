package com.saurab.atomicledger.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
	UUID id,
	String ownerReference,
	String currency,
	BigDecimal availableBalance,
	String status
) {
}
