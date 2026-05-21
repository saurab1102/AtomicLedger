package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuditLogResponse", description = "Audit log entry returned by the audit log inspection API.")
public record AuditLogResponse(
	@Schema(description = "Audit log identifier.", example = "8f746c9e-3788-4d51-9b25-6fca97a92107")
	UUID id,
	@Schema(description = "Audit action that was recorded.", example = "TRANSFER_SUCCEEDED")
	String action,
	@Schema(description = "Entity type associated with the audit event.", example = "TRANSACTION")
	String entityType,
	@Schema(description = "Entity identifier associated with the audit event.", example = "3d1a54ca-cad9-4ef4-a35d-09ff258f6b48")
	String entityId,
	@Schema(description = "Additional audit metadata captured for the event.")
	Map<String, Object> metadata,
	@Schema(description = "When the audit log was created.", example = "2026-05-21T00:00:00Z")
	Instant createdAt
) {
}
