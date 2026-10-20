package com.nitchaya.notify.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nitchaya.notify.api.repository.NotificationRepository;
import com.nitchaya.notify.api.repository.OutboxRepository;
import com.nitchaya.notify.api.web.CreateNotificationRequest;
import com.nitchaya.notify.common.NotificationChannel;
import com.nitchaya.notify.common.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test
    void createsNotificationAndOutboxAtomically() {
        var notifications = mock(NotificationRepository.class);
        var outbox = mock(OutboxRepository.class);
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var service = new NotificationService(notifications, outbox, mapper);

        var request = new CreateNotificationRequest(
                "person@example.com", "Welcome", "Hello",
                Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH), null, Map.of("tenant", "demo"));

        var created = service.create(request, "request-123");

        assertThat(created.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(created.getChannels()).contains("EMAIL").contains("PUSH");
        verify(notifications).save(created);
        verify(outbox).save(any());
    }

    @Test
    void reusesExistingNotificationForSameIdempotencyKey() {
        var notifications = mock(NotificationRepository.class);
        var outbox = mock(OutboxRepository.class);
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var service = new NotificationService(notifications, outbox, mapper);
        var existing = mock(com.nitchaya.notify.api.domain.NotificationEntity.class);
        when(notifications.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        var request = new CreateNotificationRequest("a@b.com", null, "x", Set.of(NotificationChannel.EMAIL), null, Map.of());
        assertThat(service.create(request, "same-key")).isSameAs(existing);
        verify(outbox, never()).save(any());
    }
}
