package com.saurab.atomicledger.wallet;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

	List<AuditLog> findAllByOrderByCreatedAtDescIdDesc();

	List<AuditLog> findAllByEntityTypeOrderByCreatedAtDescIdDesc(AuditEntityType entityType);

	List<AuditLog> findAllByEntityIdOrderByCreatedAtDescIdDesc(String entityId);

	List<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(AuditEntityType entityType, String entityId);
}
