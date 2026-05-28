package com.afterApp.after.controller;

import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.service.EventServices;
import com.afterApp.after.utils.TokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {
    @Autowired
    private EventServices eventServices;

    @Autowired
    private TokenUtil tokenUtil;

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
                                                "usersCount": 1,
                                                "address": {
                                                    "id": 4,
                                                    "street": "Carrer Marina",
                                                    "streetNum": "25",
                                                    "city": "Barcelona",
                                                    "province": "Catalonia",
                                                    "postalCode": "08005",
                                                    "aditionalInfo": "Industrial warehouse near the beach"
                                                }
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
                                            "message": "Access denied"
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
                                            "message": "Unexpected server error",
                                            "path": "/events"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> getAllEvents(
            @RequestHeader String authorization,
            @RequestParam(required = false)EventType type,
            @RequestParam(required = false)MusicStyle style
    ){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
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

        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
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
                                                "usersCount": 1,
                                                "address": {
                                                    "id": 4,
                                                    "street": "Carrer Marina",
                                                    "streetNum": "25",
                                                    "city": "Barcelona",
                                                    "province": "Catalonia",
                                                    "postalCode": "08005",
                                                    "aditionalInfo": "Industrial warehouse near the beach"
                                                }
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
                                            "message": "Access denied"
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
                                      "error": "NotFoundException",
                                      "message": "Usuario no encontrado",
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
                                            "message": "Unexpected server error",
                                            "path": "/events/1"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> getEventById(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try {
            return ResponseEntity.ok(eventServices.getEvent(id));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
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
                                            "usersCount": 1,
                                            "address": {
                                                "id": 4,
                                                "street": "Carrer Marina",
                                                "streetNum": "25",
                                                "city": "Barcelona",
                                                "province": "Catalonia",
                                                "postalCode": "08005",
                                                "aditionalInfo": "Industrial warehouse near the beach"
                                            }
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
                                            "message": "Access denied"
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
                                            "message": "User not found",
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
                                            "message": "Unexpected server error",
                                            "path": "/events"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> createEvent(@Valid @RequestBody CreateEventDTO dto, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.createEvent(dto, authorization));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch(BadRequestException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
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
                                            "usersCount": 2,
                                            "address": {
                                                "id": 4,
                                                "street": "Carrer Marina",
                                                "streetNum": "25",
                                                "city": "Barcelona",
                                                "province": "Catalonia",
                                                "postalCode": "08005",
                                                "aditionalInfo": "Industrial warehouse near the beach"
                                            }
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
                                                    "message": "User already joined this event"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Event Full",
                                            value = """
                                                {
                                                    "message": "Event is already full"
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
                                            "message": "Access denied"
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
                                            "message": "Event not found",
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
                                            "message": "Unexpected server error",
                                            "path": "/events/6/join"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> JoinEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.joinEvent(authorization, id));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch(BadRequestException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
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
                                            "usersCount": 1,
                                            "address": {
                                                "id": 4,
                                                "street": "Carrer Marina",
                                                "streetNum": "25",
                                                "city": "Barcelona",
                                                "province": "Catalonia",
                                                "postalCode": "08005",
                                                "aditionalInfo": "Industrial warehouse near the beach"
                                            }
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
                                                    "message": "User is not part of this event"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Host Cannot Leave",
                                            value = """
                                                {
                                                    "message": "Host cannot leave their own event"
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
                                            "message": "Access denied"
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
                                            "message": "Event not found",
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
                                            "message": "Unexpected server error",
                                            "path": "/events/6/leave"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> leaveEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.leaveEvent(authorization, id));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch (BadRequestException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
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
                                            "usersCount": 3,
                                            "address": {
                                                "id": 4,
                                                "street": "Carrer Marina",
                                                "streetNum": "25",
                                                "city": "Barcelona",
                                                "province": "Catalonia",
                                                "postalCode": "08005",
                                                "aditionalInfo": "Industrial warehouse near the beach"
                                            }
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
                                                    "message": "User is already in the event"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Event Full",
                                            value = """
                                                {
                                                    "message": "Event capacity is full"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Host Error",
                                            value = """
                                                {
                                                    "message": "Host already in the event"
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
                                            "message": "Access denied"
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
                                            "message": "Only host can invite users"
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
                                            "message": "User not found",
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
                                            "message": "Unexpected server error",
                                            "path": "/events/6/invite/user/3"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> inviteUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.inviteUser(authorization, eventId, userId));
        }catch (UnauthorizedException e){
            return ResponseEntity.status(403).body(e.getMessage());
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
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
                                            "usersCount": 2,
                                            "address": {
                                                "id": 4,
                                                "street": "Carrer Marina",
                                                "streetNum": "25",
                                                "city": "Barcelona",
                                                "province": "Catalonia",
                                                "postalCode": "08005",
                                                "aditionalInfo": "Industrial warehouse near the beach"
                                            }
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
                                                    "message": "Host cannot be kicked from the event"
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
                                            "message": "Access denied"
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
                                            "message": "Only host can kick users"
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
                                            "message": "User not found",
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
                                            "message": "Unexpected server error",
                                            "path": "/events/6/kick/user/3"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> kickUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization ){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.kickUser(authorization, eventId, userId));
        }catch (UnauthorizedException e){
            return ResponseEntity.status(403).body(e.getMessage());
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
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
                                            "message": "Access denied"
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
                                            "message": "Only host can delete the event"
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
                                            "message": "Event not found",
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
                                            "message": "Unexpected server error",
                                            "path": "/events/6"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            eventServices.deleteEvent(id, authorization);
            return ResponseEntity.noContent().build();
        }catch (UnauthorizedException e){
            return ResponseEntity.status(403).body(e.getMessage());
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }




    //Gestionar exceptions y docs



}
