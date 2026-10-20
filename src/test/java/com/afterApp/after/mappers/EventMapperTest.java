package com.afterApp.after.mappers;

import com.afterApp.after.dto.AddressDTO;
import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.UpdateEventDTO;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {
    @Test
    void shouldMapCreationResponseAndPartialUpdates() {
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
        var event = EventMapper.fromDto(dto);
        event.setId(1L);
        Users host = new Users();
        host.setDisplayName("host");
        event.setHost(host);
        assertEquals("Marina", event.getAddress().getStreet());
        assertEquals("Floor 2", event.getAddress().getAditionalInfo());
        var response = EventMapper.toDto(event);
        assertEquals("host", response.getHostDisplayName());
        assertEquals(0, response.getUsersCount());
        assertEquals("Party", response.getName());
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
}
