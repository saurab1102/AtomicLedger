package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
	String errorCode,
	String message,
	List<ApiErrorDetailResponse> details,
	Instant timestamp
) {
}
