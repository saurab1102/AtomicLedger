package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
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

	@Query("""
		select t.id
		from WalletTransaction t
		left join LedgerEntry le on le.transaction = t
		where t.status = com.saurab.atomicledger.wallet.WalletTransactionStatus.SUCCEEDED
		  and t.transactionType = com.saurab.atomicledger.wallet.WalletTransactionType.TRANSFER
		group by t.id
		having count(le.id) <> 2
		   or sum(case when le.entryType = com.saurab.atomicledger.wallet.LedgerEntryType.DEBIT then 1 else 0 end) <> 1
		   or sum(case when le.entryType = com.saurab.atomicledger.wallet.LedgerEntryType.CREDIT then 1 else 0 end) <> 1
		order by t.id
		""")
	List<UUID> findTransferStructureMismatchTransactionIds(Pageable pageable);

	@Query("""
		select t.id
		from WalletTransaction t
		left join LedgerEntry le on le.transaction = t
		where t.status = com.saurab.atomicledger.wallet.WalletTransactionStatus.SUCCEEDED
		  and t.transactionType = com.saurab.atomicledger.wallet.WalletTransactionType.TRANSFER
		group by t.id
		having coalesce(sum(case when le.entryType = com.saurab.atomicledger.wallet.LedgerEntryType.DEBIT then le.amount else 0 end), 0)
		    <> coalesce(sum(case when le.entryType = com.saurab.atomicledger.wallet.LedgerEntryType.CREDIT then le.amount else 0 end), 0)
		order by t.id
		""")
	List<UUID> findTransferAmountMismatchTransactionIds(Pageable pageable);

	@Query("""
		select t.id
		from WalletTransaction t
		left join LedgerEntry le on le.transaction = t
		where t.status = com.saurab.atomicledger.wallet.WalletTransactionStatus.SUCCEEDED
		  and t.transactionType = com.saurab.atomicledger.wallet.WalletTransactionType.DEPOSIT
		group by t.id
		having count(le.id) <> 1
		   or sum(case when le.entryType = com.saurab.atomicledger.wallet.LedgerEntryType.CREDIT then 1 else 0 end) <> 1
		order by t.id
		""")
	List<UUID> findDepositStructureMismatchTransactionIds(Pageable pageable);

	interface WalletLedgerBalanceSummary {
		UUID getWalletId();

		BigDecimal getDerivedBalance();
	}
}
