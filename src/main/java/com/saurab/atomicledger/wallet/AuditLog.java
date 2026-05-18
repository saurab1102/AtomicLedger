package com.saurab.atomicledger.wallet;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64)
	private AuditAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false, length = 64)
	private AuditEntityType entityType;

	@Column(name = "entity_id", nullable = false, length = 255)
	private String entityId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AuditLog() {
	}

	public AuditLog(
		UUID id,
		AuditAction action,
		AuditEntityType entityType,
		String entityId,
		Map<String, Object> metadata,
		Instant createdAt
	) {
		this.id = id;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.metadata = metadata;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return this.id;
	}

	public AuditAction getAction() {
		return this.action;
	}

	public AuditEntityType getEntityType() {
		return this.entityType;
	}

	public String getEntityId() {
		return this.entityId;
	}

	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}
}
