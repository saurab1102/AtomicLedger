package com.saurab.atomicledger.wallet.api;

import java.util.List;

public record WalletTransactionHistoryPageResponse(
	List<WalletTransactionHistoryItemResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
