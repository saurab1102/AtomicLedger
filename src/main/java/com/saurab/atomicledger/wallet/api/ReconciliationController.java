package com.saurab.atomicledger.wallet.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.ReconciliationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(name = "Reconciliation", description = "Accounting reconciliation APIs.")
public class ReconciliationController {

	private final ReconciliationService reconciliationService;

	public ReconciliationController(ReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	@PostMapping("/run")
	@Operation(summary = "Run reconciliation", description = "Runs reconciliation checks against persisted wallet, ledger, and transfer data.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "Reconciliation run completed.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ReconciliationResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "Missing or invalid API key.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public ReconciliationResponse run() {
		return this.reconciliationService.run();
	}
}
