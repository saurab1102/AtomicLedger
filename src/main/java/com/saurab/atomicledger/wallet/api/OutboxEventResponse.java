package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxEventResponse(
	UUID id,
	String eventType,
	String aggregateType,
	String aggregateId,
	Map<String, Object> payload,
	String status,
	int attemptCount,
	Instant createdAt,
	Instant publishedAt,
	String lastError
) {
}
