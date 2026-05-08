package com.saurab.atomicledger.wallet;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
