package com.afterApp.after.controller;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.exceptions.InvalidTokenException;
import com.afterApp.after.service.UserAccessServices;
import com.afterApp.after.utils.TokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, JWT generation and logout")
public class UserAccessController {
    @Autowired
    UserAccessServices userAccessServices;

    @Autowired
    private TokenUtil tokenUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates user access credentials and a public user profile. New users receive the FREE role by default.")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "Usuario creado correctamente"
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (username already exists or invalid data)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Username Exists",
                                            value = """
                                                {
                                                    "status": 400,
                                                    "error": "Bad Request",
                                                    "message": "Username already exists",
                                                    "path": "/v1/auth/register",
                                                    "timestamp": "2026-08-11T13:25:00"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Validation Error",
                                            value = """
                                                {
                                                    "username": "Username required",
                                                    "password": "Password must contain at least 6 characters",
                                                    "name": "Name required",
                                                    "lastname": "LastName required",
                                                    "email": "Email required",
                                                    "phoneNumber": "Phone number required"
                                                }
                                                """
                                    )
                            }
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
                                            "path": "/v1/auth/register"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody LoginDTO dto){
        try{
            userAccessServices.registerUser(dto);
            return ResponseEntity.ok("Usuario creado correctamente");
        }catch (DataIntegrityViolationException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user and generate JWT token", description = "Validates credentials and returns a JWT as plain text.")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful, token generated",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "eyJhbGciOiJIUzI1NiJ9..."
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Access denied",
                                            "path": "/v1/auth/login",
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
                                            "path": "/v1/auth/login"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> createToken(@RequestBody LoginDTO dto){
        if (userAccessServices.validateUser(dto)) {
            String token = tokenUtil.generateToken((dto.getUsername()));
            return ResponseEntity.ok(token);
        }else{
            throw new InvalidTokenException("Access denied");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the current JWT for the running application instance.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = ""
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
                                            "path": "/v1/auth/logout",
                                            "timestamp": "2026-08-11T13:25:00"
                                        }
                                        """
                            )
                    )
            )
    })
   public ResponseEntity<?> logOutUser(@RequestHeader String authorization){
        tokenUtil.invalidateToken(authorization);

        return ResponseEntity.ok("");
    }

}
