package com.saurab.atomicledger.wallet.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.ReconciliationService;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

	private final ReconciliationService reconciliationService;

	public ReconciliationController(ReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	@PostMapping("/run")
	public ReconciliationResponse run() {
		return this.reconciliationService.run();
	}
}
