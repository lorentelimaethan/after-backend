package com.afterApp.after.controller;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.entity.Users;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.service.UserServices;
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

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserServices userServices;

    @Autowired
    TokenUtil tokenUtil;

    //Falta get de UserDetails

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 7,
                                            "name": "Ethan",
                                            "lastname": "Lorente",
                                            "email": "ethanlo@gmail.com",
                                            "phoneNumber": "+34111111222",
                                            "displayName": "ethanlo2"
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
                    description = "User not found",
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
                                            "path": "/users/7"
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
                                            "path": "/users/7"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> getUserById(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(userServices.getUserById(id));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 7,
                                            "name": "Ethan",
                                            "lastname": "Lorente",
                                            "email": "ethanlo@gmail.com",
                                            "phoneNumber": "+34111111222",
                                            "displayName": "ethanlo2"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (invalid data or unauthorized update attempt)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Bad Request",
                                    value = """
                                        {
                                            "message": "You can only update your own profile"
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
                    description = "User not found",
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
                                            "path": "/users/7"
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
                                            "path": "/users/7"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO uDetails, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try {
            return ResponseEntity.ok(userServices.updateUser(id, uDetails, authorization));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/display-name")
    @Operation(summary = "Update user display name")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Display name updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "id": 7,
                                            "name": "Ethan",
                                            "lastname": "Lorente",
                                            "email": "ethanlo@gmail.com",
                                            "phoneNumber": "+34111111222",
                                            "displayName": "newDisplayName"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (invalid format or validation error)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid Format",
                                            value = """
                                                {
                                                    "message": "Incorrect username format"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Bad Request",
                                            value = """
                                                {
                                                    "message": "Display name cannot be empty"
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
                    description = "User not found",
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
                                            "path": "/users/7/display-name"
                                        }
                                        """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Display name already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Conflict",
                                    value = """
                                        {
                                            "message": "Display name already exists"
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
                                            "path": "/users/7/display-name"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> updateDisplayName(@PathVariable Long id, @RequestHeader String authorization, @Valid @RequestBody UpdateDisplayNameDTO uDetails){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(userServices.updateDisplayName(id, authorization, uDetails));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (BadRequestException | FormatRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (AlreadyExistsException e){
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
