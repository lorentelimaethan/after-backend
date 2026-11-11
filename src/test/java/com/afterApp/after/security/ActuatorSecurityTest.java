package com.afterApp.after.security;

import com.afterApp.after.support.HttpTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorSecurityTest extends HttpTestBase {

    @Test
    void shouldAllowHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectMetricsWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMetricsForFreeUser() throws Exception {
        createUser("free", "FREE");

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", tokenFor("free")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMetricsForAdmin() throws Exception {
        createUser("admin", "ADMIN");

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isOk());
    }
}
