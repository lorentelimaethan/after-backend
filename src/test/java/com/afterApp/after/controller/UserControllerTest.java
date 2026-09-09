package com.afterApp.after.controller;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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
    void shouldGetUserByIdSuccessfully() throws Exception {
        String token = createUserToken("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(get("/users/{id}", userId)
                        .header("authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.displayName").value("ethanlo"));
    }

    @Test
    void shouldRejectGetUserWhenTokenIsInvalid() throws Exception {
        createUser("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(get("/users/{id}", userId)
                        .header("authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        String token = createUserToken("ethanlo");

        mockMvc.perform(get("/users/{id}", 999L)
                        .header("authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateOwnUserProfileSuccessfully() throws Exception {
        String token = createUserToken("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(put("/users/{id}", userId)
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ethan",
                                  "lastname": "Lorente",
                                  "email": "ethan@example.com",
                                  "phoneNumber": "+34612345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ethan"))
                .andExpect(jsonPath("$.lastname").value("Lorente"))
                .andExpect(jsonPath("$.email").value("ethan@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+34612345678"));
    }

    @Test
    void shouldRejectUpdateUserWithInvalidEmail() throws Exception {
        String token = createUserToken("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(put("/users/{id}", userId)
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUpdateOtherUserProfile() throws Exception {
        String token = createUserToken("ethanlo");
        createUser("otheruser");
        Long otherUserId = userId("otheruser");

        mockMvc.perform(put("/users/{id}", otherUserId)
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Intruder"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only update your own profile"));
    }

    @Test
    void shouldUpdateDisplayNameSuccessfully() throws Exception {
        String token = createUserToken("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(patch("/users/{id}/display-name", userId)
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "after.host"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("after.host"));
    }

    @Test
    void shouldRejectInvalidDisplayName() throws Exception {
        String token = createUserToken("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(patch("/users/{id}/display-name", userId)
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "_bad"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenDisplayNameAlreadyExists() throws Exception {
        String token = createUserToken("ethanlo");
        createUser("takenname");
        Long userId = userId("ethanlo");

        mockMvc.perform(patch("/users/{id}/display-name", userId)
                        .header("authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "takenname"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already Existing username"));
    }

    @Test
    void shouldUpdateUserRoleSuccessfully() throws Exception {
        String adminToken = createUserToken("adminuser", "ADMIN");
        createUser("targetuser", "FREE");
        Long targetUserId = userId("targetuser");

        mockMvc.perform(patch("/users/{id}/role", targetUserId)
                        .header("authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "PREMIUM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId))
                .andExpect(jsonPath("$.displayName").value("targetuser"))
                .andExpect(jsonPath("$.roleName").value("PREMIUM"));
    }

    @Test
    void shouldRejectUpdateRoleWhenRequesterDoesNotHavePermission() throws Exception {
        String freeToken = createUserToken("freeuser", "FREE");
        createUser("targetuser", "FREE");
        Long targetUserId = userId("targetuser");

        mockMvc.perform(patch("/users/{id}/role", targetUserId)
                        .header("authorization", bearer(freeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "PREMIUM"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only users with UPDATE_ROLE can update roles"));
    }

    @Test
    void shouldRejectUpdateRoleWithInvalidBody() throws Exception {
        String adminToken = createUserToken("adminuser", "ADMIN");
        createUser("targetuser", "FREE");
        Long targetUserId = userId("targetuser");

        mockMvc.perform(patch("/users/{id}/role", targetUserId)
                        .header("authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingToUnknownRole() throws Exception {
        String adminToken = createUserToken("adminuser", "ADMIN");
        createUser("targetuser", "FREE");
        Long targetUserId = userId("targetuser");

        mockMvc.perform(patch("/users/{id}/role", targetUserId)
                        .header("authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteOwnUserProfileSuccessfully() throws Exception {
        String token = createUserToken("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(patch("/users/{id}", userId)
                        .header("authorization", bearer(token)))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(userId));
    }

    @Test
    void shouldRejectDeletingAnotherUserProfile() throws Exception {
        String token = createUserToken("ethanlo");
        createUser("otheruser");
        Long otherUserId = userId("otheruser");

        mockMvc.perform(patch("/users/{id}", otherUserId)
                        .header("authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only delete your own profile"));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsById(otherUserId));
    }

    @Test
    void shouldRejectDeleteUserWhenTokenIsInvalid() throws Exception {
        createUser("ethanlo");
        Long userId = userId("ethanlo");

        mockMvc.perform(patch("/users/{id}", userId)
                        .header("authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    private String createUserToken(String username) {
        createUser(username);
        return tokenUtil.generateToken(username);
    }

    private String createUserToken(String username, String roleName) {
        createUser(username, roleName);
        return tokenUtil.generateToken(username);
    }

    private void createUser(String username) {
        createUser(username, null);
    }

    private void createUser(String username, String roleName) {
        Users user = new Users();
        user.setDisplayName(username);
        if (roleName != null) {
            user.setUserRole(role(roleName));
        }

        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("unused-in-controller-tests");
        access.setUser(user);

        userAccessRepository.save(access);
    }

    private UserRole role(String roleName) {
        return userRoleRepository.findByRoleName(roleName).orElseThrow();
    }

    private Long userId(String username) {
        return userAccessRepository.findByUsername(username).orElseThrow().getUser().getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
