package com.afterApp.after.controller;

import com.afterApp.after.support.HttpTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends HttpTestBase {

    // ---------- GET profile ----------

    @Test
    void shouldGetUserById() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(get("/after/v1/users/{id}", userId)
                        .header("Authorization", tokenFor("ethan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.displayName").value("ethan"));
    }

    @Test
    void shouldShowPrivateDataToOwner() throws Exception {
        Long ownerId = createUserWithPersonalData("owner");

        mockMvc.perform(get("/after/v1/users/{id}", ownerId)
                        .header("Authorization", tokenFor("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Owner"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("123456789"));
    }

    @Test
    void shouldHidePrivateDataFromOtherUsers() throws Exception {
        Long ownerId = createUserWithPersonalData("owner");
        createUser("other");

        mockMvc.perform(get("/after/v1/users/{id}", ownerId)
                        .header("Authorization", tokenFor("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("owner"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.lastname").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.roleName").doesNotExist());
    }

    @Test
    void shouldHidePrivateDataFromOtherUsersEvenAfterOwnerReadIt() throws Exception {
        Long ownerId = createUserWithPersonalData("owner");
        createUser("other");
        mockMvc.perform(get("/after/v1/users/{id}", ownerId)
                        .header("Authorization", tokenFor("owner")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/after/v1/users/{id}", ownerId)
                        .header("Authorization", tokenFor("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void shouldShowPrivateDataToOwnerEvenAfterOtherUserReadIt() throws Exception {
        Long ownerId = createUserWithPersonalData("owner");
        createUser("other");
        mockMvc.perform(get("/after/v1/users/{id}", ownerId)
                        .header("Authorization", tokenFor("other")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/after/v1/users/{id}", ownerId)
                        .header("Authorization", tokenFor("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        createUser("ethan");

        mockMvc.perform(get("/after/v1/users/{id}", 999L)
                        .header("Authorization", tokenFor("ethan")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectGetUserWithInvalidToken() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(get("/after/v1/users/{id}", userId)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldRejectGetUserWithoutToken() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(get("/after/v1/users/{id}", userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    // ---------- PUT profile ----------

    @Test
    void shouldUpdateOwnProfile() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(put("/after/v1/users/{id}", userId)
                        .header("Authorization", tokenFor("ethan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ethan", "lastname": "Lorente",
                                 "email": "ethan@example.com", "phoneNumber": "+34612345678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ethan"))
                .andExpect(jsonPath("$.lastname").value("Lorente"))
                .andExpect(jsonPath("$.email").value("ethan@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+34612345678"));
    }

    @Test
    void shouldRejectProfileUpdateWithInvalidEmail() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(put("/after/v1/users/{id}", userId)
                        .header("Authorization", tokenFor("ethan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUpdatingAnotherProfile() throws Exception {
        createUser("ethan");
        Long otherId = createUser("other").getId();

        mockMvc.perform(put("/after/v1/users/{id}", otherId)
                        .header("Authorization", tokenFor("ethan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Intruder\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only update your own profile"));
    }

    // ---------- PATCH display name ----------

    @Test
    void shouldUpdateDisplayName() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(patch("/after/v1/users/{id}/display-name", userId)
                        .header("Authorization", tokenFor("ethan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"after.host\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("after.host"));
    }

    @Test
    void shouldRejectInvalidDisplayName() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(patch("/after/v1/users/{id}/display-name", userId)
                        .header("Authorization", tokenFor("ethan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"_bad\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenDisplayNameIsTaken() throws Exception {
        Long userId = createUser("ethan").getId();
        createUser("takenname");

        mockMvc.perform(patch("/after/v1/users/{id}/display-name", userId)
                        .header("Authorization", tokenFor("ethan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"takenname\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already Existing username"));
    }

    // ---------- PATCH role ----------

    @Test
    void shouldUpdateRoleAsAdmin() throws Exception {
        createUser("admin", "ADMIN");
        Long targetId = createUser("target", "FREE").getId();

        mockMvc.perform(patch("/after/v1/users/{id}/role", targetId)
                        .header("Authorization", tokenFor("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\": \"PREMIUM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.roleName").value("PREMIUM"));
    }

    @Test
    void shouldRejectRoleUpdateWithoutPermission() throws Exception {
        createUser("free", "FREE");
        Long targetId = createUser("target", "FREE").getId();

        mockMvc.perform(patch("/after/v1/users/{id}/role", targetId)
                        .header("Authorization", tokenFor("free"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\": \"PREMIUM\"}"))
                .andExpect(status().isForbidden());

        assertEquals("FREE", userRepository.findById(targetId).orElseThrow().getUserRole().getRoleName());
    }

    @Test
    void shouldRejectRoleUpdateWithEmptyRoleName() throws Exception {
        createUser("admin", "ADMIN");
        Long targetId = createUser("target").getId();

        mockMvc.perform(patch("/after/v1/users/{id}/role", targetId)
                        .header("Authorization", tokenFor("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForUnknownRole() throws Exception {
        createUser("admin", "ADMIN");
        Long targetId = createUser("target").getId();

        mockMvc.perform(patch("/after/v1/users/{id}/role", targetId)
                        .header("Authorization", tokenFor("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\": \"UNKNOWN\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE account ----------

    @Test
    void shouldDeleteOwnAccount() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(delete("/after/v1/users/{id}", userId)
                        .header("Authorization", tokenFor("ethan")))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(userId));
    }

    @Test
    void shouldRejectTokenOfDeletedUser() throws Exception {
        Long userId = createUser("deleted").getId();
        String token = tokenFor("deleted");
        mockMvc.perform(delete("/after/v1/users/{id}", userId)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/after/v1/users/{id}", userId)
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDeletingAnotherAccount() throws Exception {
        createUser("ethan");
        Long otherId = createUser("other").getId();

        mockMvc.perform(delete("/after/v1/users/{id}", otherId)
                        .header("Authorization", tokenFor("ethan")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own profile"));

        assertTrue(userRepository.existsById(otherId));
    }

    @Test
    void shouldRejectDeleteWithInvalidToken() throws Exception {
        Long userId = createUser("ethan").getId();

        mockMvc.perform(delete("/after/v1/users/{id}", userId)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    // ---------- helpers ----------

    private Long createUserWithPersonalData(String username) throws Exception {
        Long userId = createUser(username).getId();
        mockMvc.perform(put("/after/v1/users/{id}", userId)
                        .header("Authorization", tokenFor(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Owner", "lastname": "Example",
                                 "email": "owner@example.com", "phoneNumber": "123456789"}
                                """))
                .andExpect(status().isOk());
        return userId;
    }
}
