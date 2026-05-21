package com.saurab.atomicledger.wallet.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OutboxEventResponse", description = "Outbox event exposed by the inspection API.")
public record OutboxEventResponse(
	@Schema(description = "Outbox event identifier.", example = "44444444-4444-4444-4444-444444444444")
	UUID id,
	@Schema(description = "Event type.", example = "TRANSFER_SUCCEEDED")
	String eventType,
	@Schema(description = "Aggregate type associated with the event.", example = "TRANSACTION")
	String aggregateType,
	@Schema(description = "Aggregate identifier associated with the event.", example = "3d1a54ca-cad9-4ef4-a35d-09ff258f6b48")
	String aggregateId,
	@Schema(description = "Event payload persisted in the outbox.")
	Map<String, Object> payload,
	@Schema(description = "Current publishing status.", example = "PENDING")
	String status,
	@Schema(description = "Number of publish attempts.", example = "0")
	int attemptCount,
	@Schema(description = "When the outbox event was created.", example = "2026-05-21T00:00:00Z")
	Instant createdAt,
	@Schema(description = "When the outbox event was published, if already published.", example = "2026-05-21T00:00:05Z", nullable = true)
	Instant publishedAt,
	@Schema(description = "Last publishing error, if any.", nullable = true)
	String lastError
) {
}
