package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
	UUID id,
	String action,
	String entityType,
	String entityId,
	Map<String, Object> metadata,
	Instant createdAt
) {
}
