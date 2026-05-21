package com.saurab.atomicledger.wallet.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorDetail", description = "Field-level or header-level error detail.")
public record ApiErrorDetailResponse(
	@Schema(description = "Field or header associated with the error.", example = "amount")
	String field,
	@Schema(description = "Human-readable validation or domain error detail.", example = "amount must be positive")
	String message
) {
}
