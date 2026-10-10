package com.nerlogix.controller;

import com.nerlogix.dto.BroadcastNotificationRequest;
import com.nerlogix.dto.NotificationDto;
import com.nerlogix.entity.User;
import com.nerlogix.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final AlertService alertService;

    public NotificationController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(alertService.getUserNotifications(currentUser, pageable));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        alertService.markNotificationRead(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User currentUser) {
        alertService.markAllRead(currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> broadcast(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BroadcastNotificationRequest req) {
        alertService.broadcast(req.titles(), req.messages(), currentUser);
        return ResponseEntity.ok().build();
    }
}
