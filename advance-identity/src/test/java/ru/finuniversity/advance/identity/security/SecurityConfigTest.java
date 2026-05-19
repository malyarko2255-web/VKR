package ru.finuniversity.advance.identity.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.finuniversity.advance.identity.config.SecurityConfig;
import ru.finuniversity.advance.identity.controller.AuthController;
import ru.finuniversity.advance.identity.controller.UserController;
import ru.finuniversity.advance.identity.repository.UserRepository;
import ru.finuniversity.advance.identity.service.AuditLogService;
import ru.finuniversity.advance.identity.service.UserSyncService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, AuthController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired MockMvc mockMvc;

    // Отключаем реальный JwkSet lookup
    @MockBean JwtDecoder     jwtDecoder;
    @MockBean UserSyncService  userSyncService;
    @MockBean UserRepository   userRepository;
    @MockBean AuditLogService  auditLogService;

    @Test
    void publicAuthEndpoint_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withDispatcherRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/anything")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"))))
                .andExpect(status().isForbidden());
    }
}
