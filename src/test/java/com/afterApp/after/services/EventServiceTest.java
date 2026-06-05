package com.afterApp.after.services;

import com.afterApp.after.dto.AddressDTO;
import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.service.EventServices;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    void shouldCreateEventSuccessfully() {

        AddressDTO addressDto = new AddressDTO();
        addressDto.setStreet("Diagonal");
        addressDto.setStreetNum("123");
        addressDto.setCity("Barcelona");
        addressDto.setProvince("Catalonia");
        addressDto.setPostalCode("08001");
        addressDto.setAdditionalInfo("Near beach");

        CreateEventDTO dto = new CreateEventDTO();
        dto.setName("After Party");
        dto.setDescription("Techno night");
        dto.setCapacity(100);
        dto.setDateTime(LocalDateTime.now().plusDays(10));
        dto.setEventType(EventType.CHILL);
        dto.setMusicStyle(MusicStyle.HOUSE);
        dto.setAddress(addressDto);

        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        UserAccess access = new UserAccess();
        access.setUsername("Host");
        access.setUser(host);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Host");

        when(userAccessRepository.findByUsername("Host"))
                .thenReturn(Optional.of(access));

        when(eventRepository.save(any(Events.class)))
                .thenAnswer(invocation -> {

                    Events saved = invocation.getArgument(0);
                    saved.setId(10L);

                    return saved;
                });

        EventResponseDTO result =
                eventServices.createEvent(dto, "fake-token");

        assertEquals("After Party", result.getName());
        assertEquals(100, result.getCapacity());
        assertEquals("Host", result.getHostDisplayName());
    }

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
        user.setDisplayName("Requester");

        UserAccess access = new UserAccess();
        access.setUsername("Requester");
        access.setUser(user);

        Users host = new Users();
        host.setId(2L);
        host.setDisplayName("host");

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Requester");

        when(userAccessRepository.findByUsername("Requester"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(eventRepository.save(any(Events.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result =
                eventServices.joinEvent("fake-token", 10L);

        assertEquals(1, result.getUsersCount());
        assertTrue(event.getUsers().contains(user));
    }

    @Test
    void shouldThrowUserAlreadyJoinedEvent(){
        Users user = new Users();
        user.setId(1L);
        user.setDisplayName("Requester");

        UserAccess access = new UserAccess();
        access.setUsername("Requester");
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
                .thenReturn("Requester");

        when(userAccessRepository.findByUsername("Requester"))
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
        requester.setDisplayName("Requester");

        UserAccess access = new UserAccess();
        access.setUsername("Requester");
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
                .thenReturn("Requester");

        when(userAccessRepository.findByUsername("Requester"))
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

    @Test
    void ShouldLeaveEventSuccessfully(){
        Users user = new Users();
        user.setId(1L);
        user.setDisplayName("Requester");

        UserAccess access = new UserAccess();
        access.setUsername("Requester");
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
                .thenReturn("Requester");

        when(userAccessRepository.findByUsername("Requester"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(eventRepository.save(any(Events.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result =
                eventServices.leaveEvent("fake-token", 10L);

        assertFalse(event.getUsers().contains(user));

        assertEquals(0, result.getUsersCount());
    }

    @Test
    void ShouldThrowWhenHostTriesToLeaveEvent(){
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
                () -> eventServices.leaveEvent("fake-token", 10L)
        );

        assertEquals(
                "Host cannot leave own event",
                exception.getMessage()
        );

    }

    @Test
    void ShouldThrowWhenUserIsNotInEvent(){
        Users user = new Users();
        user.setId(1L);
        user.setDisplayName("Requester");

        Users host = new Users();
        host.setId(2L);
        host.setDisplayName("Host");

        UserAccess access = new UserAccess();
        access.setUsername("Requester");
        access.setUser(user);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Requester");

        when(userAccessRepository.findByUsername("Requester"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventServices.leaveEvent("fake-token", 10L)
        );

        assertEquals(
                "User not in event",
                exception.getMessage()
        );

    }

    @Test
    void ShouldInviteUserSuccessfully(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        Users target = new Users();
        target.setId(2L);
        target.setDisplayName("Invited");

        UserAccess hostAccess = new UserAccess();
        hostAccess.setUsername("Host");
        hostAccess.setUser(host);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Host");

        when(userAccessRepository.findByUsername("Host"))
                .thenReturn(Optional.of(hostAccess));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(target));

        when(eventRepository.save(any(Events.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result =
                eventServices.inviteUser("fake-token", 10L, 2L);

        assertEquals(1, result.getUsersCount());

        assertTrue(event.getUsers().contains(target));
    }

    @Test
    void shouldThrowWhenUserIsNotHostTryingToInvite(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        Users normalUser = new Users();
        normalUser.setId(2L);
        normalUser.setDisplayName("Normal");

        Users target;
        target = new Users();
        target.setId(3L);

        UserAccess access = new UserAccess();
        access.setUsername("Normal");
        access.setUser(normalUser);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(10);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Normal");

        when(userAccessRepository.findByUsername("Normal"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> eventServices.inviteUser("fake-token", 10L, 3L)
        );

        assertEquals(
                "Only host can invite Users",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenUserAlreadyInEvent(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        Users target = new Users();
        target.setId(2L);
        target.setDisplayName("Target");

        UserAccess hostAccess = new UserAccess();
        hostAccess.setUsername("Host");
        hostAccess.setUser(host);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);

        event.getUsers().add(target);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Host");

        when(userAccessRepository.findByUsername("Host"))
                .thenReturn(Optional.of(hostAccess));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(target));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> eventServices.inviteUser("fake-token", 10L, 2L)
        );

        assertEquals(
                "User is already in the Event",
                exception.getMessage()
        );


    }

    @Test
    void shouldKickUserSuccessfully(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        Users target = new Users();
        target.setId(2L);
        target.setDisplayName("Target");

        UserAccess access = new UserAccess();
        access.setUsername("Host");
        access.setUser(host);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.getUsers().add(target);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Host");

        when(userAccessRepository.findByUsername("Host"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(target));

        when(eventRepository.save(any(Events.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result =
                eventServices.kickUser("fake-token", 10L, 2L);

        assertEquals(0, result.getUsersCount());

        assertFalse(event.getUsers().contains(target));
    }

    @Test
    void shouldThrowWhenNonHostTriesToKickUser(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        Users normalUser = new Users();
        normalUser.setId(2L);
        normalUser.setDisplayName("Normal");

        Users target;
        target = new Users();
        target.setId(3L);

        UserAccess access = new UserAccess();
        access.setUsername("Normal");
        access.setUser(normalUser);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Normal");

        when(userAccessRepository.findByUsername("Normal"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> eventServices.kickUser("fake-token", 10L, 3L)
        );

        assertEquals(
                "Only host can delete Users",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenTryingToKickHost(){
        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        UserAccess access = new UserAccess();
        access.setUsername("Host");
        access.setUser(host);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("Host");

        when(userAccessRepository.findByUsername("Host"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(host));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> eventServices.kickUser("fake-token", 10L, 1L)
        );

        assertEquals(
                "Host can not be kicked",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenUserIsNotInEvent() {

        Users host = new Users();
        host.setId(1L);
        host.setDisplayName("Host");

        Users target = new Users();
        target.setId(2L);
        target.setDisplayName("Target");

        UserAccess access = new UserAccess();
        access.setUsername("host");
        access.setUser(host);

        Events event = new Events();
        event.setId(10L);
        event.setHost(host);

        when(tokenUtil.extractUsername("fake-token"))
                .thenReturn("host");

        when(userAccessRepository.findByUsername("host"))
                .thenReturn(Optional.of(access));

        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(target));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventServices.kickUser("fake-token", 10L, 2L)
        );

        assertEquals(
                "User is not in the Event",
                exception.getMessage()
        );
    }
}
