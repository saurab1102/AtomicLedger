package com.saurab.atomicledger.wallet.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.AuditLog;
import com.saurab.atomicledger.wallet.AuditLogService;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Audit log inspection APIs.")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	@Operation(summary = "List audit logs", description = "Returns audit log entries with optional filtering by entity type and entity identifier.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "Audit logs returned successfully.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = AuditLogResponse.class)))
		)
	})
	public List<AuditLogResponse> list(
		@Parameter(description = "Optional entity type filter.", example = "TRANSACTION")
		@RequestParam(required = false) String entityType,
		@Parameter(description = "Optional entity identifier filter.", example = "55555555-5555-5555-5555-555555555555")
		@RequestParam(required = false) String entityId
	) {
		return this.auditLogService.findLogs(entityType, entityId).stream()
			.map(this::toResponse)
			.toList();
	}

	private AuditLogResponse toResponse(AuditLog auditLog) {
		return new AuditLogResponse(
			auditLog.getId(),
			auditLog.getAction().name(),
			auditLog.getEntityType().name(),
			auditLog.getEntityId(),
			auditLog.getMetadata(),
			auditLog.getCreatedAt()
		);
	}
}
