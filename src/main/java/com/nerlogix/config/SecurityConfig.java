package com.nerlogix.config;

import com.nerlogix.security.ApiKeyAuthenticationFilter;
import com.nerlogix.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final String allowedOriginsStr;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            @Value("${nerlogix.cors.allowed-origins:https://ner-logix-frontend.vercel.app,http://localhost:5173,http://localhost:8080}") String allowedOriginsStr) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.allowedOriginsStr = allowedOriginsStr;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type", "Event-Stream"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://maps.googleapis.com https://*.googleapis.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; img-src 'self' data: blob: https://*.basemaps.cartocdn.com https://*.tile.openstreetmap.org https://maps.gstatic.com https://*.googleapis.com https://*.google.com; font-src 'self' data: https://fonts.gstatic.com; frame-src 'self' https://www.google.com https://maps.google.com https://*.google.com; connect-src 'self' ws: wss: https://maps.googleapis.com https://*.googleapis.com;"))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"path\":\"" + request.getRequestURI() + "\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Allow all OPTIONS preflight requests immediately
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public & health endpoints
                .requestMatchers(
                    "/api/health",
                    "/health",
                    "/api/public/**",
                    "/public/**",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/auth/register",
                    "/auth/login",
                    "/auth/refresh",
                    "/api/docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/error",
                    "/",
                    "/index.html",
                    "/static/**",
                    "/assets/**",
                    "/*.js",
                    "/*.css",
                    "/*.png",
                    "/*.svg",
                    "/*.ico",
                    "/manifest.json"
                ).permitAll()

                // Integrations endpoints (authenticated via ApiKeyAuthenticationFilter)
                .requestMatchers("/api/integrations/**", "/integrations/**").permitAll()

                // Admin endpoints
                .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")

                // Moderator & Admin endpoints
                .requestMatchers(HttpMethod.PATCH, "/api/roads/*/status", "/roads/*/status").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/field-reports/*/verify", "/field-reports/*/verify").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/field-reports/*/reject", "/field-reports/*/reject").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/incidents/*/resolve", "/incidents/*/resolve").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/alerts/*/ack", "/alerts/*/ack").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/alerts/*/resolve", "/alerts/*/resolve").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/notifications/broadcast", "/notifications/broadcast").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/settings/emergency", "/settings/emergency").hasAnyRole("MODERATOR", "ADMIN")

                // Authenticated endpoints for all roles
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/auth/**").authenticated()
                .requestMatchers("/realtime/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
