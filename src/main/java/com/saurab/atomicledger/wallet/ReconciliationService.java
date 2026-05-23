package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saurab.atomicledger.wallet.api.ReconciliationFailedCheckResponse;
import com.saurab.atomicledger.wallet.api.ReconciliationResponse;

@Service
public class ReconciliationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliationService.class);
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
	private static final int MAX_FAILED_CHECKS_PER_TYPE = 100;

	private final WalletRepository walletRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final AuditLogService auditLogService;
	private final OutboxEventService outboxEventService;
	private final OperationalMetrics operationalMetrics;

	public ReconciliationService(
		WalletRepository walletRepository,
		LedgerEntryRepository ledgerEntryRepository,
		AuditLogService auditLogService,
		OutboxEventService outboxEventService,
		OperationalMetrics operationalMetrics
	) {
		this.walletRepository = walletRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.auditLogService = auditLogService;
		this.outboxEventService = outboxEventService;
		this.operationalMetrics = operationalMetrics;
	}

	@Transactional
	public ReconciliationResponse run() {
		List<ReconciliationFailedCheckResponse> failedChecks = new ArrayList<>();
		Map<String, Integer> failedCheckCounts = new HashMap<>();

		runTimedCheck("wallet", failedChecks, () -> reconcileWalletBalances(failedChecks, failedCheckCounts));
		runTimedCheck("transfer", failedChecks, () -> reconcileTransfers(failedChecks, failedCheckCounts));
		runTimedCheck("deposit", failedChecks, () -> reconcileDeposits(failedChecks, failedCheckCounts));

		ReconciliationStatus status = failedChecks.isEmpty() ? ReconciliationStatus.PASS : ReconciliationStatus.FAIL;
		this.operationalMetrics.incrementReconciliationRuns();
		this.auditLogService.recordInCurrentTransaction(
			AuditAction.RECONCILIATION_RUN,
			AuditEntityType.RECONCILIATION,
			"reconciliation",
			Map.of("status", status.name(), "failedCheckCount", failedChecks.size())
		);
		if (status == ReconciliationStatus.FAIL) {
			this.operationalMetrics.incrementReconciliationFailures();
			this.auditLogService.recordInCurrentTransaction(
				AuditAction.RECONCILIATION_FAILED,
				AuditEntityType.RECONCILIATION,
				"reconciliation",
				Map.of("failedCheckCount", failedChecks.size())
			);
			this.outboxEventService.recordInCurrentTransaction(
				OutboxEventType.RECONCILIATION_FAILED,
				OutboxAggregateType.RECONCILIATION,
				"reconciliation",
				Map.of("status", status.name(), "failedCheckCount", failedChecks.size())
			);
		}
		logReconciliationOutcome(status, failedChecks.size());
		return new ReconciliationResponse(status.name(), failedChecks);
	}

	private void runTimedCheck(String phaseName, List<ReconciliationFailedCheckResponse> failedChecks, Runnable check) {
		long startedAt = System.nanoTime();
		int failedChecksBefore = failedChecks.size();
		check.run();
		long durationInMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
		logReconciliationPhaseDuration(phaseName, durationInMillis, failedChecks.size() - failedChecksBefore);
	}

	private void logReconciliationPhaseDuration(String phaseName, long durationInMillis, int addedFailedChecks) {
		LOGGER.atInfo()
			.addKeyValue("phase", phaseName)
			.addKeyValue("durationMs", durationInMillis)
			.addKeyValue("addedFailedChecks", addedFailedChecks)
			.log("reconciliation_phase_completed");
	}

	private void logReconciliationOutcome(ReconciliationStatus status, int failedCheckCount) {
		LOGGER.atInfo()
			.addKeyValue("reconciliationStatus", status.name())
			.addKeyValue("failedCheckCount", failedCheckCount)
			.log("reconciliation_completed");
	}

	private void reconcileWalletBalances(
		List<ReconciliationFailedCheckResponse> failedChecks,
		Map<String, Integer> failedCheckCounts
	) {
		// Wallet balances are cached state; reconciliation proves they still match the ledger truth.
		Map<UUID, BigDecimal> derivedBalancesByWalletId = this.ledgerEntryRepository.summarizeDerivedBalancesByWallet().stream()
			.collect(Collectors.toMap(
				LedgerEntryRepository.WalletLedgerBalanceSummary::getWalletId,
				summary -> summary.getDerivedBalance().setScale(2)
			));

		for (Wallet wallet : this.walletRepository.findAll()) {
			BigDecimal derivedBalance = derivedBalancesByWalletId.getOrDefault(wallet.getId(), ZERO);
			if (wallet.getAvailableBalance().compareTo(derivedBalance) != 0) {
				addFailedCheck(failedChecks, failedCheckCounts, new ReconciliationFailedCheckResponse(
					"WALLET_BALANCE_MISMATCH",
					"WALLET",
					wallet.getId().toString(),
					"wallet available balance does not match ledger-derived balance"
				));
			}
		}
	}

	private void reconcileTransfers(
		List<ReconciliationFailedCheckResponse> failedChecks,
		Map<String, Integer> failedCheckCounts
	) {
		// A successful transfer must remain a balanced double-entry movement: one debit and one credit.
		PageRequest limitedRows = PageRequest.of(0, MAX_FAILED_CHECKS_PER_TYPE);

		for (UUID transactionId : this.ledgerEntryRepository.findTransferStructureMismatchTransactionIds(limitedRows)) {
			addFailedCheck(failedChecks, failedCheckCounts, new ReconciliationFailedCheckResponse(
					"TRANSFER_LEDGER_STRUCTURE_MISMATCH",
					"TRANSACTION",
					transactionId.toString(),
					"successful transfer must have exactly one DEBIT and one CREDIT ledger entry"
				));
		}

		for (UUID transactionId : this.ledgerEntryRepository.findTransferAmountMismatchTransactionIds(limitedRows)) {
			addFailedCheck(failedChecks, failedCheckCounts, new ReconciliationFailedCheckResponse(
					"TRANSFER_LEDGER_AMOUNT_MISMATCH",
					"TRANSACTION",
					transactionId.toString(),
					"successful transfer must have equal total DEBIT and CREDIT amounts"
				));
		}
	}

	private void reconcileDeposits(
		List<ReconciliationFailedCheckResponse> failedChecks,
		Map<String, Integer> failedCheckCounts
	) {
		// Successful deposits are expected to create exactly one credit entry for the target wallet.
		PageRequest limitedRows = PageRequest.of(0, MAX_FAILED_CHECKS_PER_TYPE);
		for (UUID transactionId : this.ledgerEntryRepository.findDepositStructureMismatchTransactionIds(limitedRows)) {
			addFailedCheck(failedChecks, failedCheckCounts, new ReconciliationFailedCheckResponse(
					"DEPOSIT_LEDGER_STRUCTURE_MISMATCH",
					"TRANSACTION",
					transactionId.toString(),
					"successful deposit must have exactly one CREDIT ledger entry"
				));
		}
	}

	private void addFailedCheck(
		List<ReconciliationFailedCheckResponse> failedChecks,
		Map<String, Integer> failedCheckCounts,
		ReconciliationFailedCheckResponse failedCheck
	) {
		int countForType = failedCheckCounts.getOrDefault(failedCheck.checkType(), 0);
		if (countForType < MAX_FAILED_CHECKS_PER_TYPE) {
			failedChecks.add(failedCheck);
		}
		failedCheckCounts.put(failedCheck.checkType(), countForType + 1);
	}
}
