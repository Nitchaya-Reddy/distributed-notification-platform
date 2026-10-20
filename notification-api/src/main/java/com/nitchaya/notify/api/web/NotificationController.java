package com.nitchaya.notify.api.web;

import com.nitchaya.notify.api.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var entity = service.create(request, idempotencyKey);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/notifications/" + entity.getId()))
                .body(NotificationResponse.from(entity));
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return NotificationResponse.from(service.get(id));
    }
}
