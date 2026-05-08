package com.saurab.atomicledger.wallet.api;

import java.util.List;

public record ValidationErrorResponse(
	String message,
	List<FieldErrorResponse> errors
) {
}
