package ru.finuniversity.advance.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.finuniversity.advance.identity.dto.AuditLogEntry;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    @Async("auditExecutor")
    public void log(AuditLogEntry entry) {
        try {
            jdbcTemplate.update(
                "INSERT INTO audit_log " +
                "(request_id, user_id, username, role, endpoint, http_method, " +
                " status_code, ip_address, user_agent, duration_ms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?::inet, ?, ?)",
                entry.requestId(),
                entry.userId(),
                entry.username(),
                entry.role(),
                entry.endpoint(),
                entry.httpMethod(),
                entry.statusCode(),
                entry.ipAddress(),
                entry.userAgent(),
                entry.durationMs()
            );
        } catch (Exception e) {
            log.error("Failed to persist audit log entry for requestId={}", entry.requestId(), e);
        }
    }
}
