package com.saurab.atomicledger.wallet.api;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet creation, deposits, and wallet transaction history APIs.")
public class WalletController {

	private final WalletService walletService;

	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create wallet", description = "Creates a new active INR wallet for the supplied owner reference.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "Wallet created successfully.",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = WalletResponse.class),
				examples = @ExampleObject(
					name = "walletCreated",
					value = """
						{
						  "id": "11111111-1111-1111-1111-111111111111",
						  "ownerReference": "customer-123",
						  "currency": "INR",
						  "availableBalance": 0.00,
						  "status": "ACTIVE"
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "Validation or currency error.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
		return this.walletService.createWallet(request);
	}

	@PostMapping("/{walletId}/deposit")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Deposit funds", description = "Deposits funds into an existing active wallet.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "Deposit created successfully.",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = DepositResponse.class),
				examples = @ExampleObject(
					name = "depositSucceeded",
					value = """
						{
						  "transactionId": "33333333-3333-3333-3333-333333333333",
						  "walletId": "11111111-1111-1111-1111-111111111111",
						  "amount": 125.50,
						  "currency": "INR",
						  "transactionType": "DEPOSIT",
						  "transactionStatus": "SUCCEEDED",
						  "availableBalance": 125.50
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "Validation, missing idempotency key, or unsupported currency.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "Wallet not found.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public DepositResponse deposit(
		@Parameter(description = "Wallet identifier.", example = "11111111-1111-1111-1111-111111111111")
		@PathVariable UUID walletId,
		@Parameter(description = "Client-provided idempotency key used to safely retry the same deposit request.", required = true, example = "deposit-001")
		@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
		@Valid @RequestBody DepositWalletRequest request
	) {
		return this.walletService.deposit(walletId, idempotencyKey, request);
	}

	@GetMapping("/{walletId}/transactions")
	@Operation(summary = "Get wallet transaction history", description = "Returns paginated transaction history for a wallet based on its ledger participation.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "Wallet transaction history returned successfully.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = WalletTransactionHistoryPageResponse.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "Wallet not found.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public WalletTransactionHistoryPageResponse transactionHistory(
		@Parameter(description = "Wallet identifier.", example = "11111111-1111-1111-1111-111111111111")
		@PathVariable UUID walletId,
		@Parameter(description = "Zero-based page number.", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "Number of history items per page.", example = "20")
		@RequestParam(defaultValue = "20") int size,
		@Parameter(description = "Sort clause in the form field,direction.", example = "createdAt,desc")
		@RequestParam(defaultValue = "createdAt,desc") String sort
	) {
		return this.walletService.getTransactionHistory(walletId, page, size, sort);
	}
}
