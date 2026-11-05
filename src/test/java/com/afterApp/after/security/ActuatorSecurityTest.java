package com.afterApp.after.security;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserAccessRepository userAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userAccessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAllowHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectMetricsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMetricsForUserWithoutViewMetricsAuthority() throws Exception {
        String freeToken = createUserToken("freeuser", "FREE");

        mockMvc.perform(get("/actuator/metrics")
                        .header("authorization", bearer(freeToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMetricsForAdminWithViewMetricsAuthority() throws Exception {
        String adminToken = createUserToken("adminuser", "ADMIN");

        mockMvc.perform(get("/actuator/metrics")
                        .header("authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    private String createUserToken(String username, String roleName) {
        Users user = new Users();
        user.setDisplayName(username);
        user.setUserRole(role(roleName));

        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("unused-in-security-tests");
        access.setUser(user);

        userAccessRepository.save(access);
        return tokenUtil.generateToken(username);
    }

    private UserRole role(String roleName) {
        return userRoleRepository.findByRoleName(roleName).orElseThrow();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
