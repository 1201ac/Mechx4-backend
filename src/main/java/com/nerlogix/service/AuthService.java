package com.nerlogix.service;

import com.nerlogix.dto.*;
import com.nerlogix.entity.RefreshToken;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.Role;
import com.nerlogix.repository.RefreshTokenRepository;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.security.JwtTokenProvider;
import com.nerlogix.security.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginRateLimiter rateLimiter;
    private final SseService sseService;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            LoginRateLimiter rateLimiter,
            SseService sseService,
            @Value("${nerlogix.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.rateLimiter = rateLimiter;
        this.sseService = sseService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        validatePassword(req.password());

        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalStateException("Email already registered");
        }

        User user = new User(
            req.email(),
            passwordEncoder.encode(req.password()),
            req.name(),
            Role.USER
        );
        if (req.language() != null && !req.language().isBlank()) {
            user.setLanguage(req.language());
        }
        user = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = generateRawRefreshToken();
        saveRefreshToken(user, rawRefreshToken, UUID.randomUUID().toString());

        UserDto userDto = UserDto.fromEntity(user);
        if (sseService != null) {
            sseService.sendToRole("user.registered", userDto, Role.ADMIN, Role.MODERATOR);
        }

        return new AuthResponse(accessToken, rawRefreshToken, userDto);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, String ipAddress) {
        if (!rateLimiter.isAllowed(ipAddress)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Please try again later.");
        }

        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(ipAddress);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Account is deactivated");
        }

        rateLimiter.reset(ipAddress);

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = generateRawRefreshToken();
        
        saveRefreshToken(user, rawRefreshToken, UUID.randomUUID().toString());

        return new AuthResponse(accessToken, rawRefreshToken, UserDto.fromEntity(user));
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest req) {
        String tokenHash = hashToken(req.refreshToken());
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        RefreshToken token = tokenOpt.get();

        if (Boolean.TRUE.equals(token.getRevoked())) {
            if (token.getFamilyId() != null) {
                refreshTokenRepository.deleteByFamilyId(token.getFamilyId());
            } else {
                refreshTokenRepository.deleteByUser(token.getUser());
            }
            throw new BadCredentialsException("Refresh token reuse detected");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException("Refresh token has expired");
        }

        User user = token.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Account is deactivated");
        }

        // Rotate token within family
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String newRawRefreshToken = generateRawRefreshToken();
        String familyId = token.getFamilyId() != null ? token.getFamilyId() : UUID.randomUUID().toString();
        saveRefreshToken(user, newRawRefreshToken, familyId);

        String newAccessToken = tokenProvider.generateAccessToken(user);

        return new TokenRefreshResponse(newAccessToken, newRawRefreshToken);
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public UserDto updateProfile(User user, UpdateProfileRequest req) {
        if (req.name() != null && !req.name().isBlank()) {
            user.setName(req.name());
        }
        if (req.language() != null && !req.language().isBlank()) {
            user.setLanguage(req.language());
        }
        if (req.alertPreferences() != null) {
            user.setAlertPreferences(req.alertPreferences());
        }
        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest req) {
        validatePassword(req.newPassword());
        if (!passwordEncoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 ||
            !UPPERCASE_PATTERN.matcher(password).find() ||
            !LOWERCASE_PATTERN.matcher(password).find() ||
            !DIGIT_PATTERN.matcher(password).find() ||
            !SPECIAL_PATTERN.matcher(password).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
    }

    private String generateRawRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    private void saveRefreshToken(User user, String rawToken, String familyId) {
        String hash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt, familyId, false));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
