package com.saurab.atomicledger.wallet.api;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Wallet-to-wallet transfer APIs.")
public class TransferController {

	private final WalletService walletService;

	public TransferController(WalletService walletService) {
		this.walletService = walletService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create transfer", description = "Transfers funds between two existing active wallets.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "Transfer created successfully.",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = TransferResponse.class),
				examples = @ExampleObject(
					name = "transferSucceeded",
					value = """
						{
						  "transactionId": "55555555-5555-5555-5555-555555555555",
						  "sourceWalletId": "11111111-1111-1111-1111-111111111111",
						  "destinationWalletId": "22222222-2222-2222-2222-222222222222",
						  "amount": 75.00,
						  "currency": "INR",
						  "transactionType": "TRANSFER",
						  "transactionStatus": "SUCCEEDED",
						  "sourceAvailableBalance": 125.00,
						  "destinationAvailableBalance": 75.00
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "Validation, missing idempotency key, unsupported currency, or invalid transfer target.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "One or both wallets not found.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "409",
			description = "Source wallet has insufficient balance.",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = ApiErrorResponse.class),
				examples = @ExampleObject(
					name = "insufficientBalance",
					value = """
						{
						  "errorCode": "INSUFFICIENT_BALANCE",
						  "message": "insufficient available balance",
						  "details": [
						    {
						      "field": "amount",
						      "message": "insufficient available balance"
						    }
						  ],
						  "timestamp": "2026-05-21T00:00:00Z"
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "Missing or invalid API key.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public TransferResponse createTransfer(
		@Parameter(description = "Client-provided idempotency key used to safely retry the same transfer request.", required = true, example = "transfer-001")
		@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
		@Valid @RequestBody CreateTransferRequest request
	) {
		return this.walletService.transfer(idempotencyKey, request);
	}
}
