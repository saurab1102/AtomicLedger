package com.saurab.atomicledger.wallet.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.AuditLog;
import com.saurab.atomicledger.wallet.AuditLogService;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	public List<AuditLogResponse> list(
		@RequestParam(required = false) String entityType,
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
