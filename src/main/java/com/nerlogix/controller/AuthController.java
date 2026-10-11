package com.nerlogix.controller;

import com.nerlogix.dto.*;
import com.nerlogix.entity.User;
import com.nerlogix.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
        return ResponseEntity.ok(authService.login(req, ip));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        if (user != null) {
            authService.logout(user);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateMe(@AuthenticationPrincipal User user, @RequestBody UpdateProfileRequest req) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.updateProfile(user, req));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest req) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        authService.changePassword(user, req);
        return ResponseEntity.noContent().build();
    }
}
