package com.saurab.atomicledger.wallet.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletTransactionHistoryPageResponse", description = "Paginated wallet transaction history response.")
public record WalletTransactionHistoryPageResponse(
	@Schema(description = "Wallet transaction history items on the current page.")
	List<WalletTransactionHistoryItemResponse> content,
	@Schema(description = "Zero-based page number.", example = "0")
	int page,
	@Schema(description = "Requested page size.", example = "20")
	int size,
	@Schema(description = "Total number of matching history items.", example = "3")
	long totalElements,
	@Schema(description = "Total number of pages.", example = "1")
	int totalPages
) {
}
