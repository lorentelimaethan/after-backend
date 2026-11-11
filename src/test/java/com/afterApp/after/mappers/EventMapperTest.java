package com.afterApp.after.mappers;

import com.afterApp.after.dto.AddressDTO;
import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.dto.UpdateEventDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventMapperTest {

    @Test
    void shouldMapCreateDtoToEvent() {
        Events event = EventMapper.fromDto(createDto());

        assertEquals("Party", event.getName());
        assertEquals(10, event.getCapacity());
        assertEquals(EventType.AFTER, event.getEventType());
        assertEquals(MusicStyle.TECHNO, event.getMusicStyle());
    }

    @Test
    void shouldMapAddressFromCreateDto() {
        Events event = EventMapper.fromDto(createDto());

        assertEquals("Marina", event.getAddress().getStreet());
        assertEquals("Barcelona", event.getAddress().getCity());
        assertEquals("Floor 2", event.getAddress().getAditionalInfo());
    }

    @Test
    void shouldMapEventToResponseDto() {
        Events event = EventMapper.fromDto(createDto());
        event.setId(1L);
        event.setHost(host("host"));
        event.getUsers().add(host("guest"));

        EventResponseDTO response = EventMapper.toDto(event);

        assertEquals(1L, response.getId());
        assertEquals("Party", response.getName());
        assertEquals("host", response.getHostDisplayName());
        assertEquals(1, response.getUsersCount());
    }

    @Test
    void shouldUpdateOnlyFieldsSentInUpdateDto() {
        Events event = EventMapper.fromDto(createDto());
        UpdateEventDTO update = new UpdateEventDTO();
        update.setName("Updated");
        update.setMusicStyle(MusicStyle.HOUSE);

        EventMapper.updateEventData(event, update);

        assertEquals("Updated", event.getName());
        assertEquals(MusicStyle.HOUSE, event.getMusicStyle());
        assertEquals("Description", event.getDescription());
        assertEquals(10, event.getCapacity());
        assertEquals("Marina", event.getAddress().getStreet());
    }

    private Users host(String displayName) {
        Users user = new Users();
        user.setDisplayName(displayName);
        return user;
    }

    private CreateEventDTO createDto() {
        AddressDTO address = new AddressDTO();
        address.setStreet("Marina");
        address.setStreetNum("25");
        address.setPostalCode("08005");
        address.setCity("Barcelona");
        address.setProvince("Barcelona");
        address.setAdditionalInfo("Floor 2");

        CreateEventDTO dto = new CreateEventDTO();
        dto.setName("Party");
        dto.setDescription("Description");
        dto.setCapacity(10);
        dto.setDateTime(LocalDateTime.of(2030, 1, 1, 22, 0));
        dto.setEventType(EventType.AFTER);
        dto.setMusicStyle(MusicStyle.TECHNO);
        dto.setAddress(address);
        return dto;
    }
}
