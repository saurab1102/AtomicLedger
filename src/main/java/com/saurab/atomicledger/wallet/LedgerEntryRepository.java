package com.saurab.atomicledger.wallet;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

	List<LedgerEntry> findAllByTransactionId(UUID transactionId);
}
