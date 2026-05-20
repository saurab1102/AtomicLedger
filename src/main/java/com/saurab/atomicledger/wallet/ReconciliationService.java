package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saurab.atomicledger.wallet.api.ReconciliationFailedCheckResponse;
import com.saurab.atomicledger.wallet.api.ReconciliationResponse;

@Service
public class ReconciliationService {

	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

	private final WalletRepository walletRepository;
	private final WalletTransactionRepository walletTransactionRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final AuditLogService auditLogService;
	private final OutboxEventService outboxEventService;

	public ReconciliationService(
		WalletRepository walletRepository,
		WalletTransactionRepository walletTransactionRepository,
		LedgerEntryRepository ledgerEntryRepository,
		AuditLogService auditLogService,
		OutboxEventService outboxEventService
	) {
		this.walletRepository = walletRepository;
		this.walletTransactionRepository = walletTransactionRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.auditLogService = auditLogService;
		this.outboxEventService = outboxEventService;
	}

	@Transactional
	public ReconciliationResponse run() {
		List<ReconciliationFailedCheckResponse> failedChecks = new ArrayList<>();

		reconcileWalletBalances(failedChecks);
		reconcileTransfers(failedChecks);
		reconcileDeposits(failedChecks);

		ReconciliationStatus status = failedChecks.isEmpty() ? ReconciliationStatus.PASS : ReconciliationStatus.FAIL;
		this.auditLogService.recordInCurrentTransaction(
			AuditAction.RECONCILIATION_RUN,
			AuditEntityType.RECONCILIATION,
			"reconciliation",
			Map.of("status", status.name(), "failedCheckCount", failedChecks.size())
		);
		if (status == ReconciliationStatus.FAIL) {
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
		return new ReconciliationResponse(status.name(), failedChecks);
	}

	private void reconcileWalletBalances(List<ReconciliationFailedCheckResponse> failedChecks) {
		// Wallet balances are cached state; reconciliation proves they still match the ledger truth.
		Map<UUID, BigDecimal> derivedBalancesByWalletId = this.ledgerEntryRepository.summarizeDerivedBalancesByWallet().stream()
			.collect(Collectors.toMap(
				LedgerEntryRepository.WalletLedgerBalanceSummary::getWalletId,
				summary -> summary.getDerivedBalance().setScale(2)
			));

		for (Wallet wallet : this.walletRepository.findAll()) {
			BigDecimal derivedBalance = derivedBalancesByWalletId.getOrDefault(wallet.getId(), ZERO);
			if (wallet.getAvailableBalance().compareTo(derivedBalance) != 0) {
				failedChecks.add(new ReconciliationFailedCheckResponse(
					"WALLET_BALANCE_MISMATCH",
					"WALLET",
					wallet.getId().toString(),
					"wallet available balance does not match ledger-derived balance"
				));
			}
		}
	}

	private void reconcileTransfers(List<ReconciliationFailedCheckResponse> failedChecks) {
		// A successful transfer must remain a balanced double-entry movement: one debit and one credit.
		for (WalletTransaction transaction : this.walletTransactionRepository.findAllByStatusAndTransactionType(
			WalletTransactionStatus.SUCCEEDED,
			WalletTransactionType.TRANSFER
		)) {
			List<LedgerEntry> entries = this.ledgerEntryRepository.findAllByTransactionId(transaction.getId());
			long debitCount = entries.stream().filter(hasEntryType(LedgerEntryType.DEBIT)).count();
			long creditCount = entries.stream().filter(hasEntryType(LedgerEntryType.CREDIT)).count();

			if (entries.size() != 2 || debitCount != 1 || creditCount != 1) {
				failedChecks.add(new ReconciliationFailedCheckResponse(
					"TRANSFER_LEDGER_STRUCTURE_MISMATCH",
					"TRANSACTION",
					transaction.getId().toString(),
					"successful transfer must have exactly one DEBIT and one CREDIT ledger entry"
				));
			}

			BigDecimal debitAmount = sumAmounts(entries, LedgerEntryType.DEBIT);
			BigDecimal creditAmount = sumAmounts(entries, LedgerEntryType.CREDIT);

			if (debitAmount.compareTo(creditAmount) != 0) {
				failedChecks.add(new ReconciliationFailedCheckResponse(
					"TRANSFER_LEDGER_AMOUNT_MISMATCH",
					"TRANSACTION",
					transaction.getId().toString(),
					"successful transfer must have equal total DEBIT and CREDIT amounts"
				));
			}
		}
	}

	private void reconcileDeposits(List<ReconciliationFailedCheckResponse> failedChecks) {
		// Successful deposits are expected to create exactly one credit entry for the target wallet.
		for (WalletTransaction transaction : this.walletTransactionRepository.findAllByStatusAndTransactionType(
			WalletTransactionStatus.SUCCEEDED,
			WalletTransactionType.DEPOSIT
		)) {
			List<LedgerEntry> entries = this.ledgerEntryRepository.findAllByTransactionId(transaction.getId());
			long creditCount = entries.stream().filter(hasEntryType(LedgerEntryType.CREDIT)).count();

			if (entries.size() != 1 || creditCount != 1) {
				failedChecks.add(new ReconciliationFailedCheckResponse(
					"DEPOSIT_LEDGER_STRUCTURE_MISMATCH",
					"TRANSACTION",
					transaction.getId().toString(),
					"successful deposit must have exactly one CREDIT ledger entry"
				));
			}
		}
	}

	private Predicate<LedgerEntry> hasEntryType(LedgerEntryType entryType) {
		return entry -> entry.getEntryType() == entryType;
	}

	private BigDecimal sumAmounts(List<LedgerEntry> entries, LedgerEntryType entryType) {
		return entries.stream()
			.filter(hasEntryType(entryType))
			.map(LedgerEntry::getAmount)
			.reduce(ZERO, BigDecimal::add)
			.setScale(2);
	}
}
