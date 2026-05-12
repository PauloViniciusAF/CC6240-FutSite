package com.futsite.controller;

import com.futsite.dto.request.LoginRequest;
import com.futsite.dto.request.RegisterRequest;
import com.futsite.dto.response.AuthResponse;
import com.futsite.dto.response.UserResponse;
import com.futsite.exception.BadRequestException;
import com.futsite.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("User not authenticated");
        }
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/athletes")
    public ResponseEntity<List<UserResponse>> athletes() {
        return ResponseEntity.ok(authService.getAllAthletes());
    }
}
