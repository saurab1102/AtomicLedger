package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standard API error response.")
public record ApiErrorResponse(
	@Schema(description = "Stable machine-readable error code.", example = "INSUFFICIENT_BALANCE")
	String errorCode,
	@Schema(description = "Human-readable error summary.", example = "insufficient available balance")
	String message,
	@Schema(description = "Optional field-level or header-level details.")
	List<ApiErrorDetailResponse> details,
	@Schema(description = "Timestamp when the error response was produced.", example = "2026-05-21T00:00:00Z")
	Instant timestamp
) {
}
