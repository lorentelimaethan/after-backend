package com.afterApp.after.integration;

import com.afterApp.after.entity.Address;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserEventDeletionTest {
    @Autowired private MockMvc mvc;
    @Autowired private EventRepository events;
    @Autowired private UserAccessRepository accesses;
    @Autowired private UserRepository users;
    @Autowired private UserRoleRepository roles;
    @Autowired private TokenUtil tokens;
    @Autowired private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        events.deleteAll();
        accesses.deleteAll();
        users.deleteAll();
    }

    @Test
    void shouldRemoveDeletedAttendeeWithoutDeletingTheEvent() throws Exception {
        Users host = createUser("host");
        Users guest = createUser("guest");
        Events event = createEvent(host);
        event.getUsers().add(guest);
        events.saveAndFlush(event);

        mvc.perform(delete("/after/v1/users/{id}", guest.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("guest")))
                .andExpect(status().isNoContent());

        assertTrue(users.findById(guest.getId()).isEmpty());
        transactions.executeWithoutResult(status -> assertEquals(0,
                events.findById(event.getId()).orElseThrow().getUsers().size()));
    }

    @Test
    void shouldHandleDeletingHostWithoutLeavingAnOrphanedEvent() throws Exception {
        Users host = createUser("host");
        Events event = createEvent(host);

        int responseStatus = mvc.perform(delete("/after/v1/users/{id}", host.getId())
                        .header("Authorization", "Bearer " + tokens.generateToken("host")))
                .andReturn().getResponse().getStatus();

        assertTrue(responseStatus == 204 || responseStatus == 409,
                "Host deletion must either complete coherently or return Conflict, not a server error");
        if (responseStatus == 409) {
            assertTrue(users.existsById(host.getId()));
            assertTrue(events.existsById(event.getId()));
        } else {
            assertTrue(users.findById(host.getId()).isEmpty());
            transactions.executeWithoutResult(status -> events.findById(event.getId())
                    .ifPresent(remaining -> assertTrue(remaining.getHost() != null
                            && !remaining.getHost().getId().equals(host.getId()))));
        }
    }

    private Users createUser(String username) {
        Users user = new Users();
        user.setDisplayName(username);
        user.setUserRole(roles.findByRoleName("FREE").orElseThrow());
        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("unused");
        access.setUser(user);
        return accesses.saveAndFlush(access).getUser();
    }

    private Events createEvent(Users host) {
        Events event = new Events();
        event.setName("Deletion test");
        event.setDescription("Deletion test");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setCapacity(2);
        event.setEventType(EventType.AFTER);
        event.setMusicStyle(MusicStyle.TECHNO);
        event.setAddress(new Address());
        event.setHost(host);
        return events.saveAndFlush(event);
    }
}
