package com.example.eventsphere.controller;

import com.example.eventsphere.dto.ApiResponse;
import com.example.eventsphere.dto.AuthDTO;
import com.example.eventsphere.service.AuthService;
import com.example.eventsphere.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostConstruct
    public void init() {
        authService.createDefaultAdmin();
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful. Awaiting admin approval.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDTO.LoginResponse>> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        AuthDTO.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDTO.UserResponse>> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        AuthDTO.UserResponse profile = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
