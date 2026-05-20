package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

	List<LedgerEntry> findAllByTransactionId(UUID transactionId);

	Page<LedgerEntry> findAllByWalletId(UUID walletId, Pageable pageable);

	@Query("""
		select le.wallet.id as walletId,
		       coalesce(sum(case when le.entryType = com.saurab.atomicledger.wallet.LedgerEntryType.CREDIT then le.amount else -le.amount end), 0)
		       as derivedBalance
		from LedgerEntry le
		group by le.wallet.id
		""")
	List<WalletLedgerBalanceSummary> summarizeDerivedBalancesByWallet();

	interface WalletLedgerBalanceSummary {
		UUID getWalletId();

		BigDecimal getDerivedBalance();
	}
}
