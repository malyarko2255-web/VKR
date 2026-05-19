package ru.finuniversity.advance.identity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.finuniversity.advance.identity.dto.AuditLogEntry;
import ru.finuniversity.advance.identity.service.AuditLogService;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(300)
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long startNano = System.nanoTime();

        try {
            chain.doFilter(request, response);
        } finally {
            try {
                long durationMs = (System.nanoTime() - startNano) / 1_000_000;

                String requestIdStr = (String) request.getAttribute(RequestIdFilter.ATTR_KEY);
                UUID   requestId    = requestIdStr != null
                        ? UUID.fromString(requestIdStr) : UUID.randomUUID();

                String userIdStr = (String) request.getAttribute(UserSyncFilter.USER_ID_ATTR);
                UUID   userId    = userIdStr != null ? UUID.fromString(userIdStr) : null;

                String username = null;
                String role     = null;
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth instanceof JwtAuthenticationToken jwtAuth) {
                    username = jwtAuth.getToken().getClaimAsString("preferred_username");
                    role = jwtAuth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(a -> a.startsWith("ROLE_"))
                            .map(a -> a.substring(5))
                            .findFirst()
                            .orElse(null);
                }

                String ipAddress = getClientIp(request);

                AuditLogEntry entry = new AuditLogEntry(
                        requestId, userId, username, role,
                        request.getRequestURI(), request.getMethod(),
                        response.getStatus(), ipAddress,
                        request.getHeader("User-Agent"), durationMs
                );

                auditLogService.log(entry);
            } catch (Exception e) {
                log.error("Failed to build audit log entry", e);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
