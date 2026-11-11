package com.afterApp.after.service;

import com.afterApp.after.dto.AddressDTO;
import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.dto.UpdateEventDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.loader.EventLoader;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicesTest {

    private static final String TOKEN = "Bearer token";

    @Mock
    private EventLoader eventLoader;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAccessRepository userAccessRepository;

    @Mock
    private TokenUtil tokenUtil;

    @InjectMocks
    private EventServices eventServices;

    private final Users host = user(1L, "host");
    private final Users guest = user(2L, "guest");

    // ---------- getEvent / getAllEvents ----------

    @Test
    void shouldReturnEventWhenItExists() {
        Events event = event(10);
        event.setName("Techno Party");
        when(eventLoader.getEventById(5L)).thenReturn(event);

        EventResponseDTO result = eventServices.getEvent(5L);

        assertEquals("Techno Party", result.getName());
        assertEquals("host", result.getHostDisplayName());
    }

    @Test
    void shouldThrowWhenEventDoesNotExist() {
        when(eventLoader.getEventById(99L)).thenThrow(new NotFoundException("Event not found"));

        assertThrows(NotFoundException.class, () -> eventServices.getEvent(99L));
    }

    @Test
    void shouldReturnAllEvents() {
        when(eventLoader.getAllEvents()).thenReturn(List.of(event(10), event(5)));

        List<EventResponseDTO> result = eventServices.getAllEvents();

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEventsByType() {
        when(eventLoader.getEventsByType(EventType.AFTER)).thenReturn(List.of(event(10)));

        List<EventResponseDTO> result = eventServices.getEventsByType(EventType.AFTER);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnEventsByStyle() {
        when(eventLoader.getEventsByMusicStyle(MusicStyle.TECHNO)).thenReturn(List.of(event(10)));

        List<EventResponseDTO> result = eventServices.getEventsByStyle(MusicStyle.TECHNO);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnEventsByTypeAndStyle() {
        when(eventLoader.getEventsByTypeAndStyle(EventType.AFTER, MusicStyle.TECHNO)).thenReturn(List.of(event(10)));

        List<EventResponseDTO> result = eventServices.getEventsByTypeAndStyle(EventType.AFTER, MusicStyle.TECHNO);

        assertEquals(1, result.size());
    }

    // ---------- createEvent ----------

    @Test
    void shouldCreateEventWithLoggedUserAsHost() {
        loggedInAs(host);
        // saveEvent returns the same event it receives
        when(eventLoader.saveEvent(any(Events.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result = eventServices.createEvent(createEventDto(), TOKEN);

        assertEquals("After Party", result.getName());
        assertEquals(100, result.getCapacity());
        assertEquals("host", result.getHostDisplayName());
    }

    // ---------- joinEvent ----------

    @Test
    void shouldJoinEvent() {
        loggedInAs(guest);
        Events event = event(10);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(eventLoader.saveEvent(event)).thenReturn(event);

        EventResponseDTO result = eventServices.joinEvent(TOKEN, 10L);

        assertEquals(1, result.getUsersCount());
        assertTrue(event.getUsers().contains(guest));
    }

    @Test
    void shouldJoinEventWhenOnlyOnePlaceIsLeft() {
        loggedInAs(guest);
        Events event = event(2, user(3L, "attendee"));
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(eventLoader.saveEvent(event)).thenReturn(event);

        EventResponseDTO result = eventServices.joinEvent(TOKEN, 10L);

        assertEquals(2, result.getUsersCount());
    }

    @Test
    void shouldThrowWhenHostJoinsOwnEvent() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.joinEvent(TOKEN, 10L));

        assertEquals("Host cannot join own event", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenEventIsFull() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(1, user(3L, "attendee")));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.joinEvent(TOKEN, 10L));

        assertEquals("Event capacity is full", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenUserAlreadyJoined() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10, guest));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.joinEvent(TOKEN, 10L));

        assertEquals("User already joined", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenJoiningEventThatDoesNotExist() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(99L)).thenThrow(new NotFoundException("Event not Found"));

        assertThrows(NotFoundException.class, () -> eventServices.joinEvent(TOKEN, 99L));
    }

    // ---------- leaveEvent ----------

    @Test
    void shouldLeaveEvent() {
        loggedInAs(guest);
        Events event = event(10, guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(eventLoader.saveEvent(event)).thenReturn(event);

        EventResponseDTO result = eventServices.leaveEvent(TOKEN, 10L);

        assertEquals(0, result.getUsersCount());
        assertFalse(event.getUsers().contains(guest));
    }

    @Test
    void shouldThrowWhenHostLeavesOwnEvent() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.leaveEvent(TOKEN, 10L));

        assertEquals("Host cannot leave own event", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenLeavingEventNotJoined() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventServices.leaveEvent(TOKEN, 10L));

        assertEquals("User not in event", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    // ---------- inviteUser ----------

    @Test
    void shouldInviteUser() {
        loggedInAs(host);
        Events event = event(10);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));
        when(eventLoader.saveEvent(event)).thenReturn(event);

        EventResponseDTO result = eventServices.inviteUser(TOKEN, 10L, 2L);

        assertEquals(1, result.getUsersCount());
        assertTrue(event.getUsers().contains(guest));
    }

    @Test
    void shouldThrowWhenNonHostInvites() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> eventServices.inviteUser(TOKEN, 10L, 3L));

        assertEquals("Only host can invite Users", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenInvitedUserIsAlreadyAttending() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10, guest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.inviteUser(TOKEN, 10L, 2L));

        assertEquals("User is already in the Event", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenHostInvitesThemselves() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));
        when(userRepository.findById(1L)).thenReturn(Optional.of(host));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.inviteUser(TOKEN, 10L, 1L));

        assertEquals("Host already in the event", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenInvitingToFullEvent() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(1, user(3L, "attendee")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.inviteUser(TOKEN, 10L, 2L));

        assertEquals("Event capacity is full", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenInvitedUserDoesNotExist() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventServices.inviteUser(TOKEN, 10L, 99L));
    }

    // ---------- kickUser ----------

    @Test
    void shouldKickUser() {
        loggedInAs(host);
        Events event = event(10, guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));
        when(eventLoader.saveEvent(event)).thenReturn(event);

        EventResponseDTO result = eventServices.kickUser(TOKEN, 10L, 2L);

        assertEquals(0, result.getUsersCount());
        assertFalse(event.getUsers().contains(guest));
    }

    @Test
    void shouldThrowWhenNonHostKicks() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> eventServices.kickUser(TOKEN, 10L, 3L));

        assertEquals("Only host can delete Users", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenKickingHost() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));
        when(userRepository.findById(1L)).thenReturn(Optional.of(host));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.kickUser(TOKEN, 10L, 1L));

        assertEquals("Host can not be kicked", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenKickedUserIsNotAttending() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));
        when(userRepository.findById(2L)).thenReturn(Optional.of(guest));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventServices.kickUser(TOKEN, 10L, 2L));

        assertEquals("User is not in the Event", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenKickedUserDoesNotExist() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventServices.kickUser(TOKEN, 10L, 99L));
    }

    // ---------- updateEvent ----------

    @Test
    void shouldUpdateEvent() {
        loggedInAs(host);
        Events event = event(5);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(eventLoader.saveEvent(event)).thenReturn(event);
        UpdateEventDTO dto = new UpdateEventDTO();
        dto.setName("Updated Event");
        dto.setCapacity(8);

        EventResponseDTO result = eventServices.updateEvent(TOKEN, 10L, dto);

        assertEquals("Updated Event", result.getName());
        assertEquals(8, result.getCapacity());
    }

    @Test
    void shouldAllowCapacityEqualToCurrentAttendees() {
        loggedInAs(host);
        Events event = event(5, guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);
        when(eventLoader.saveEvent(event)).thenReturn(event);
        UpdateEventDTO dto = new UpdateEventDTO();
        dto.setCapacity(1);

        EventResponseDTO result = eventServices.updateEvent(TOKEN, 10L, dto);

        assertEquals(1, result.getCapacity());
    }

    @Test
    void shouldThrowWhenNonHostUpdatesEvent() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> eventServices.updateEvent(TOKEN, 10L, new UpdateEventDTO()));

        assertEquals("Only hosts can update Events", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    @Test
    void shouldThrowWhenNewCapacityIsLowerThanAttendees() {
        loggedInAs(host);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10, guest, user(3L, "attendee")));
        UpdateEventDTO dto = new UpdateEventDTO();
        dto.setCapacity(1);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventServices.updateEvent(TOKEN, 10L, dto));

        assertEquals("Event capacity cannot be lower than current attendees", exception.getMessage());
        verify(eventLoader, never()).saveEvent(any());
    }

    // ---------- deleteEvent ----------

    @Test
    void shouldDeleteEventWhenRequesterIsHost() {
        loggedInAs(host);
        Events event = event(10);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event);

        eventServices.deleteEvent(10L, TOKEN);

        verify(eventLoader).deleteEvent(event);
    }

    @Test
    void shouldThrowWhenNonHostDeletesEvent() {
        loggedInAs(guest);
        when(eventLoader.getEventForUpdate(10L)).thenReturn(event(10));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> eventServices.deleteEvent(10L, TOKEN));

        assertEquals("Only host can delete own event", exception.getMessage());
        verify(eventLoader, never()).deleteEvent(any());
    }

    // ---------- helpers ----------

    private void loggedInAs(Users user) {
        UserAccess access = new UserAccess();
        access.setUsername(user.getDisplayName());
        access.setUser(user);

        when(tokenUtil.extractUsername(TOKEN)).thenReturn(user.getDisplayName());
        when(userAccessRepository.findByUsername(user.getDisplayName())).thenReturn(Optional.of(access));
    }

    private Users user(Long id, String displayName) {
        Users user = new Users();
        user.setId(id);
        user.setDisplayName(displayName);
        return user;
    }

    /** Event hosted by {@code host} with the given capacity and attendees. */
    private Events event(int capacity, Users... attendees) {
        Events event = new Events();
        event.setId(10L);
        event.setHost(host);
        event.setCapacity(capacity);
        event.getUsers().addAll(List.of(attendees));
        return event;
    }

    private CreateEventDTO createEventDto() {
        AddressDTO address = new AddressDTO();
        address.setStreet("Diagonal");
        address.setStreetNum("123");
        address.setCity("Barcelona");
        address.setProvince("Catalonia");
        address.setPostalCode("08001");

        CreateEventDTO dto = new CreateEventDTO();
        dto.setName("After Party");
        dto.setDescription("Techno night");
        dto.setCapacity(100);
        dto.setDateTime(LocalDateTime.now().plusDays(10));
        dto.setEventType(EventType.CHILL);
        dto.setMusicStyle(MusicStyle.HOUSE);
        dto.setAddress(address);
        return dto;
    }
}
