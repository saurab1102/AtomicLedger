package com.saurab.atomicledger.wallet;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public AuditLog recordInCurrentTransaction(
		AuditAction action,
		AuditEntityType entityType,
		String entityId,
		Map<String, Object> metadata
	) {
		return this.auditLogRepository.save(new AuditLog(
			UUID.randomUUID(),
			action,
			entityType,
			entityId,
			metadata,
			Instant.now()
		));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AuditLog recordStandalone(
		AuditAction action,
		AuditEntityType entityType,
		String entityId,
		Map<String, Object> metadata
	) {
		return recordInCurrentTransaction(action, entityType, entityId, metadata);
	}

	@Transactional(readOnly = true)
	public List<AuditLog> findLogs(String entityType, String entityId) {
		AuditEntityType parsedEntityType = entityType == null || entityType.isBlank()
			? null
			: AuditEntityType.valueOf(entityType.trim().toUpperCase());
		String normalizedEntityId = entityId == null || entityId.isBlank() ? null : entityId.trim();

		if (parsedEntityType != null && normalizedEntityId != null) {
			return this.auditLogRepository.findAllByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(parsedEntityType, normalizedEntityId);
		}
		if (parsedEntityType != null) {
			return this.auditLogRepository.findAllByEntityTypeOrderByCreatedAtDescIdDesc(parsedEntityType);
		}
		if (normalizedEntityId != null) {
			return this.auditLogRepository.findAllByEntityIdOrderByCreatedAtDescIdDesc(normalizedEntityId);
		}
		return this.auditLogRepository.findAllByOrderByCreatedAtDescIdDesc();
	}
}
