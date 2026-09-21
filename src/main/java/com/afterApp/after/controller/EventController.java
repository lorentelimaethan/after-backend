package com.afterApp.after.controller;

import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.dto.UpdateEventDTO;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.service.EventServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Event listing, filtering, creation, attendance and host-only moderation")
public class EventController {
    @Autowired
    private EventServices eventServices;

    @GetMapping
    @Operation(summary = "Get all events")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Events retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        [
                                            {
                                                "id": 6,
                                                "name": "Techno Underground Barcelona",
                                                "description": "Underground techno party in Barcelona",
                                                "dateTime": "2026-08-15T23:00:00",
                                                "capacity": 150,
                                                "eventType": "CHILL",
                                                "musicStyle": "HOUSE",
                                                "hostDisplayName": "ethanlo2",
                                                "usersCount": 1
                                            }
                                        ]
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> getAllEvents(
            @RequestParam(required = false)EventType type,
            @RequestParam(required = false)MusicStyle style
    ){
        List<EventResponseDTO> events;

        if(type != null && style != null){
            events = eventServices.getEventsByTypeAndStyle(type, style);

        } else if (type != null) {
            events = eventServices.getEventsByType(type);

        } else if(style != null){
            events = eventServices.getEventsByStyle(style);

        }
        else {
            events = eventServices.getAllEvents();
        }

        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an event by id")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Event retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "Techno Underground Barcelona",
                                            "description": "Underground techno party in Barcelona",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 150,
                                            "eventType": "CHILL",
                                            "musicStyle": "HOUSE",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 1
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Response Example",
                                    value = """
                                    {
                                      "timestamp": "2026-05-05T12:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Content not Found",
                                      "path": "/events/1"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/1"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> getEventById(@PathVariable Long id){
        return ResponseEntity.ok(eventServices.getEvent(id));
    }


    @PostMapping
    @Operation(summary = "Create a new event")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Event created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "Techno Underground Barcelona",
                                            "description": "Underground techno party in Barcelona",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 150,
                                            "eventType": "CHILL",
                                            "musicStyle": "HOUSE",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 1
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid event data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Bad Request",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 400,
                                            "error": "Bad Request",
                                            "message": "Event capacity must be greater than 0",
                                            "path": "/events"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Host user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> createEvent(@Valid @RequestBody CreateEventDTO dto, @RequestHeader String authorization){
        return ResponseEntity.ok(eventServices.createEvent(dto, authorization));
    }


    @PatchMapping("/{id}/join")
    @Operation(summary = "Join an event")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User joined the event successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "Techno Underground Barcelona",
                                            "description": "Underground techno party in Barcelona",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 150,
                                            "eventType": "CHILL",
                                            "musicStyle": "HOUSE",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 2
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "User already joined or event is full",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Already Joined",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "User already joined this event",
                                                    "path": "/events/6/join",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Event Full",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "Event is already full",
                                                    "path": "/events/6/join",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Event or user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events/6/join"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/6/join"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> JoinEvent(@PathVariable Long id, @RequestHeader String authorization){
        return ResponseEntity.ok(eventServices.joinEvent(authorization, id));
    }

    @PatchMapping("/{id}/leave")
    @Operation(summary = "Leave an event")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User left the event successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "Techno Underground Barcelona",
                                            "description": "Underground techno party in Barcelona",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 150,
                                            "eventType": "CHILL",
                                            "musicStyle": "HOUSE",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 1
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "User is not part of the event or host cannot leave",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "User Not Joined",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "User is not part of this event",
                                                    "path": "/events/6/leave",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Host Cannot Leave",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "Host cannot leave their own event",
                                                    "path": "/events/6/leave",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Event or user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events/6/leave"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/6/leave"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> leaveEvent(@PathVariable Long id, @RequestHeader String authorization){
        return ResponseEntity.ok(eventServices.leaveEvent(authorization, id));
    }

    @PatchMapping("/{eventId}/invite/user/{userId}")
    @Operation(summary = "Invite a user to an event (host only)")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User invited successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "Techno Underground Barcelona",
                                            "description": "Underground techno party in Barcelona",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 150,
                                            "eventType": "CHILL",
                                            "musicStyle": "HOUSE",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 3
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "User already in event, host cannot be invited, or event is full",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Already in Event",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "User is already in the event",
                                                    "path": "/events/6/invite/user/3",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Event Full",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "Event capacity is full",
                                                    "path": "/events/6/invite/user/3",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Host Error",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "Host already in the event",
                                                    "path": "/events/6/invite/user/3",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only host can invite users",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = """
                                        {
                                            "status": 403,
                                            "error": "Forbidden",
                                            "message": "Only host can invite users",
                                            "path": "/events/6/invite/user/3",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Event or user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events/6/invite/user/3"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/6/invite/user/3"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> inviteUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization){
        return ResponseEntity.ok(eventServices.inviteUser(authorization, eventId, userId));
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "Update an event", description = "Updates event details. Only the event host can update an event.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "After Updated",
                                            "description": "Updated private event description",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 80,
                                            "eventType": "AFTER",
                                            "musicStyle": "TECHNO",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 1
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid event update data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Bad Request",
                                    value = """
                                        {
                                            "status": 400,
                                            "error": "Bad Request",
                                            "message": "Event capacity must be > 0",
                                            "path": "/events/6",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events/6",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only the host can update the event",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = """
                                        {
                                            "status": 403,
                                            "error": "Forbidden",
                                            "message": "Only hosts can update Events",
                                            "path": "/events/6",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events/6",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/6",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> updateEvent(
            @PathVariable Long eventId, @Valid @RequestBody UpdateEventDTO eventDTO, @RequestHeader String authorization
    )
    {
        return ResponseEntity.ok(eventServices.updateEvent(authorization, eventId, eventDTO));
    }

    @DeleteMapping("/{eventId}/kick/user/{userId}")
    @Operation(summary = "Kick a user from an event (host only)")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User kicked successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 6,
                                            "name": "Techno Underground Barcelona",
                                            "description": "Underground techno party in Barcelona",
                                            "dateTime": "2026-08-15T23:00:00",
                                            "capacity": 150,
                                            "eventType": "CHILL",
                                            "musicStyle": "HOUSE",
                                            "hostDisplayName": "ethanlo2",
                                            "usersCount": 2
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (host cannot be kicked or invalid state)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Host Cannot Be Kicked",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "Host cannot be kicked from the event",
                                                    "path": "/events/6/kick/user/3",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only host can kick users",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = """
                                        {
                                            "status": 403,
                                            "error": "Forbidden",
                                            "message": "Only host can kick users",
                                            "path": "/events/6/kick/user/3",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Event or user not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events/6/kick/user/3"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/6/kick/user/3"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> kickUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization ){
        return ResponseEntity.ok(eventServices.kickUser(authorization, eventId, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event (host only)")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "204",
                    description = "Event deleted successfully",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/events",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only host can delete the event",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = """
                                        {
                                            "status": 403,
                                            "error": "Forbidden",
                                            "message": "Only host can delete the event",
                                            "path": "/events/6",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 404,
                                            "error": "Not Found",
                                            "message": "Content not Found",
                                            "path": "/events/6"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 500,
                                            "error": "Internal Server Error",
                                            "message": "Unexpected Server Error",
                                            "path": "/events/6"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, @RequestHeader String authorization){
        eventServices.deleteEvent(id, authorization);
        return ResponseEntity.noContent().build();
    }



}
