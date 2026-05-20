package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.saurab.atomicledger.wallet.MissingIdempotencyKeyException;
import com.saurab.atomicledger.wallet.InsufficientAvailableBalanceException;
import com.saurab.atomicledger.wallet.SameWalletTransferException;
import com.saurab.atomicledger.wallet.UnsupportedWalletCurrencyException;
import com.saurab.atomicledger.wallet.WalletCurrencyMismatchException;
import com.saurab.atomicledger.wallet.WalletNotActiveException;
import com.saurab.atomicledger.wallet.WalletNotFoundException;

@RestControllerAdvice
public class WalletExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<ApiErrorDetailResponse> details = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.sorted(Comparator.comparing(FieldError::getField))
			.map(fieldError -> new ApiErrorDetailResponse(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", details);
	}

	@ExceptionHandler(UnsupportedWalletCurrencyException.class)
	public ResponseEntity<ApiErrorResponse> handleUnsupportedWalletCurrency(UnsupportedWalletCurrencyException exception) {
		return response(
			HttpStatus.BAD_REQUEST,
			"UNSUPPORTED_CURRENCY",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse("currency", exception.getMessage()))
		);
	}

	@ExceptionHandler(MissingIdempotencyKeyException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingIdempotencyKey(MissingIdempotencyKeyException exception) {
		return response(
			HttpStatus.BAD_REQUEST,
			"MISSING_IDEMPOTENCY_KEY",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse("Idempotency-Key", exception.getMessage()))
		);
	}

	@ExceptionHandler(WalletNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleWalletNotFound(WalletNotFoundException exception) {
		return response(
			HttpStatus.NOT_FOUND,
			"WALLET_NOT_FOUND",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse(exception.getField(), exception.getMessage()))
		);
	}

	@ExceptionHandler(WalletNotActiveException.class)
	public ResponseEntity<ApiErrorResponse> handleWalletNotActive(WalletNotActiveException exception) {
		return response(
			HttpStatus.BAD_REQUEST,
			"WALLET_NOT_ACTIVE",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse(exception.getField(), exception.getMessage()))
		);
	}

	@ExceptionHandler(SameWalletTransferException.class)
	public ResponseEntity<ApiErrorResponse> handleSameWalletTransfer(SameWalletTransferException exception) {
		return response(
			HttpStatus.BAD_REQUEST,
			"INVALID_TRANSFER_TARGET",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse("destinationWalletId", exception.getMessage()))
		);
	}

	@ExceptionHandler(InsufficientAvailableBalanceException.class)
	public ResponseEntity<ApiErrorResponse> handleInsufficientAvailableBalance(InsufficientAvailableBalanceException exception) {
		return response(
			HttpStatus.CONFLICT,
			"INSUFFICIENT_BALANCE",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse("amount", exception.getMessage()))
		);
	}

	@ExceptionHandler(WalletCurrencyMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleWalletCurrencyMismatch(WalletCurrencyMismatchException exception) {
		return response(
			HttpStatus.BAD_REQUEST,
			"CURRENCY_MISMATCH",
			exception.getMessage(),
			List.of(new ApiErrorDetailResponse("currency", exception.getMessage()))
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnhandledException(Exception exception) {
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error", List.of());
	}

	private ResponseEntity<ApiErrorResponse> response(
		HttpStatus status,
		String errorCode,
		String message,
		List<ApiErrorDetailResponse> details
	) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(errorCode, message, details, Instant.now()));
	}
}
