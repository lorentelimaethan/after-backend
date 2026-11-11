package com.afterApp.after.support;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import com.afterApp.after.utils.TokenUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for the HTTP tests: starts the whole application with MockMvc,
 * cleans the database before each test and creates users, tokens and events.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class HttpTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected EventRepository eventRepository;

    @Autowired
    protected UserAccessRepository userAccessRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserRoleRepository userRoleRepository;

    @Autowired
    protected TokenUtil tokenUtil;

    @Autowired
    protected CacheManager cacheManager;

    @BeforeEach
    void cleanDatabase() {
        for (String cacheName : cacheManager.getCacheNames()) {
            cacheManager.getCache(cacheName).clear();
        }
        eventRepository.deleteAll();
        userAccessRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected Users createUser(String username) {
        return createUser(username, "FREE");
    }

    protected Users createUser(String username, String roleName) {
        Users user = new Users();
        user.setDisplayName(username);
        user.setUserRole(userRoleRepository.findByRoleName(roleName).orElseThrow());

        UserAccess access = new UserAccess();
        access.setUsername(username);
        access.setPassword("not-used-in-http-tests");
        access.setUser(user);

        return userAccessRepository.save(access).getUser();
    }

    /** Returns the value for the Authorization header: "Bearer <token>". */
    protected String tokenFor(String username) {
        return "Bearer " + tokenUtil.generateToken(username);
    }

    protected Long createEvent(String token, String name, int capacity) throws Exception {
        return createEvent(token, name, "AFTER", "TECHNO", capacity);
    }

    protected Long createEvent(String token, String name, String type, String style, int capacity) throws Exception {
        String response = mockMvc.perform(post("/after/v1/events")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(name, type, style, capacity)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(response, "$.id");
        return id.longValue();
    }

    /** Reads usersCount from GET /events/{id}. */
    protected int usersCount(Long eventId, String token) throws Exception {
        String response = mockMvc.perform(get("/after/v1/events/{id}", eventId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.usersCount");
    }

    protected String eventJson(String name, String type, String style, int capacity) {
        return """
                {
                  "name": "%s",
                  "eventType": "%s",
                  "musicStyle": "%s",
                  "description": "Test event",
                  "dateTime": "2030-06-05T23:00:00",
                  "capacity": %d,
                  "address": {
                    "street": "Carrer Marina",
                    "streetNum": "25",
                    "postalCode": "08005",
                    "city": "Barcelona",
                    "province": "Barcelona"
                  }
                }
                """.formatted(name, type, style, capacity);
    }
}
