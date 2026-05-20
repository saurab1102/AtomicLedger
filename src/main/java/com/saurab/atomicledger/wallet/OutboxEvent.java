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
@Table(name = "outbox_events")
public class OutboxEvent {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 64)
	private OutboxEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "aggregate_type", nullable = false, length = 64)
	private OutboxAggregateType aggregateType;

	@Column(name = "aggregate_id", nullable = false, length = 255)
	private String aggregateId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OutboxEventStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "last_error")
	private String lastError;

	protected OutboxEvent() {
	}

	public OutboxEvent(
		UUID id,
		OutboxEventType eventType,
		OutboxAggregateType aggregateType,
		String aggregateId,
		Map<String, Object> payload,
		OutboxEventStatus status,
		int attemptCount,
		Instant createdAt,
		Instant publishedAt,
		String lastError
	) {
		this.id = id;
		this.eventType = eventType;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.payload = payload;
		this.status = status;
		this.attemptCount = attemptCount;
		this.createdAt = createdAt;
		this.publishedAt = publishedAt;
		this.lastError = lastError;
	}

	public UUID getId() {
		return this.id;
	}

	public OutboxEventType getEventType() {
		return this.eventType;
	}

	public OutboxAggregateType getAggregateType() {
		return this.aggregateType;
	}

	public String getAggregateId() {
		return this.aggregateId;
	}

	public Map<String, Object> getPayload() {
		return this.payload;
	}

	public OutboxEventStatus getStatus() {
		return this.status;
	}

	public int getAttemptCount() {
		return this.attemptCount;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getPublishedAt() {
		return this.publishedAt;
	}

	public String getLastError() {
		return this.lastError;
	}

	public void markPublished(Instant publishedAt) {
		this.status = OutboxEventStatus.PUBLISHED;
		this.publishedAt = publishedAt;
		this.lastError = null;
	}

	public void recordPublishFailure(String lastError) {
		this.attemptCount += 1;
		this.lastError = lastError;
	}
}
