package com.saurab.atomicledger.wallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

	Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

	List<WalletTransaction> findAllByStatusAndTransactionType(
		WalletTransactionStatus status,
		WalletTransactionType transactionType
	);
}
