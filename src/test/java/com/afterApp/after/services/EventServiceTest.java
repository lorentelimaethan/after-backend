package com.afterApp.after.services;

import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.service.EventServices;
import com.afterApp.after.utils.TokenUtil;
import org.hsqldb.rights.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAccessRepository userAccessRepository;

    @Mock
    TokenUtil tokenUtil;

    @InjectMocks
    private EventServices eventServices;

    @Test
    void shouldReturnEventWhenExists(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("admin");

        Events event = new Events();
        event.setId(10L);
        event.setName("Techno Party");
        event.setHost(host);
        event.setUsers(new HashSet<>());

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        EventResponseDTO result =
                eventServices.getEvent(10L);

        assertEquals("Techno Party", result.getName());
        assertEquals("admin", result.getHostDisplayName());
    }

    @Test
    void shouldJoinEventSuccessfully(){
        Users user = new Users();
        user.setId(1L);
        user.setDisplayName("John");

        UserAccess access = new UserAccess();
        access.setUsername("Requester");
        access.setUser(user);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("John");

        when(userAccessRepository.findByUsername("John"))
                .thenReturn(Optional.of(access));

        Users host = new Users();
        host.setId(2L);
        host.setDisplayName("host");

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(eventRepository.save(any(Events.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result =
                eventServices.joinEvent("fake-token", 10L);

        assertEquals(1, result.getUsersCount());
    }

    @Test
    void shouldThrowUserAlreadyJoinedEvent(){
        Users user = new Users();
        user.setId(1L);
        user.setDisplayName("John");

        UserAccess access = new UserAccess();
        access.setUsername("John");
        access.setUser(user);

        Users host = new Users();
        host.setId(2L);
        host.setDisplayName("Host");

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        event.getUsers().add(user);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("John");

        when(userAccessRepository.findByUsername("John"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> eventServices.joinEvent("fake-token", 10L)
        );

        assertEquals(
                "User already joined",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenHostJoinsOwnEvent(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        UserAccess access = new UserAccess();
        access.setUsername("Host");
        access.setUser(host);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Host");

        when(userAccessRepository.findByUsername("Host"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> eventServices.joinEvent("fake-token", 10L)
        );

        assertEquals(
                "Host cannot join own event",
                exception.getMessage()
        );
    }

    @Test
    void ShouldThrowWhenEventIsFull(){
        Users requester = new Users();
        requester.setId(1L);
        requester.setDisplayName("John");

        UserAccess access = new UserAccess();
        access.setUsername("John");
        access.setUser(requester);

        Users host = new Users();
        host.setId(2L);
        host.setDisplayName("Host");

        Users attendee = new Users();
        attendee.setId(3L);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(1);

        event.getUsers().add(attendee);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("John");

        when(userAccessRepository.findByUsername("John"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> eventServices.joinEvent("fake-token", 10L)
        );

        assertEquals(
                "Event capacity is full",
                exception.getMessage()
        );
    }


}
