package ru.finuniversity.advance.identity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.finuniversity.advance.identity.service.UserSyncService;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class UserSyncFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTR = "userId";

    private final UserSyncService userSyncService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                UUID userId = userSyncService.syncUser(jwtAuth.getToken());
                request.setAttribute(USER_ID_ATTR, userId.toString());
            }
        } catch (Exception e) {
            log.warn("User sync failed, continuing without userId: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
