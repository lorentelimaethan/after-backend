package com.afterApp.after.controller;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.service.UserAccessServices;
import com.afterApp.after.utils.TokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
public class UserAccessController {
    @Autowired
    UserAccessServices userAccessServices;

    @Autowired
    private TokenUtil tokenUtil;

    @PostMapping("/auth/register")
    @Operation(summary = "Register a new user")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "message": "Usuario creado correctamente"
                                        }
                                        """
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
                                                    "message": "Username already exists"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Validation Error",
                                            value = """
                                                {
                                                    "message": "Username required"
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
                                            "message": "Unexpected server error",
                                            "path": "token/auth/register"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterDTO dto){
        try{
            userAccessServices.registerUser(dto);
            return ResponseEntity.ok("Usuario creado correctamente");
        }catch (BadRequestException | DataIntegrityViolationException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login user and generate JWT token")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful, token generated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                        {
                                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                        }
                                        """
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
                                            "path": "token/auth/login"
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
            return ResponseEntity.status(401).body("Access denied");
        }
    }


}
