package com.nitchaya.notify.worker.service;

import com.nitchaya.notify.common.DeliveryCommand;
import com.nitchaya.notify.worker.domain.DeliveryAttempt;
import com.nitchaya.notify.worker.repository.DeliveryAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryAttemptWriter {
    private final DeliveryAttemptRepository repository;

    public DeliveryAttemptWriter(DeliveryAttemptRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryAttempt start(DeliveryCommand command) {
        var attempt = repository.findByNotificationIdAndChannel(command.notificationId(), command.channel())
                .orElseGet(() -> DeliveryAttempt.pending(command.notificationId(), command.channel()));
        if (attempt.getStatus() != com.nitchaya.notify.worker.domain.DeliveryStatus.SENT) {
            attempt.markAttempt();
            return repository.save(attempt);
        }
        return attempt;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sent(DeliveryAttempt attempt, String providerMessageId) {
        var managed = repository.findByNotificationIdAndChannel(attempt.getNotificationId(), attempt.getChannel()).orElseThrow();
        managed.markSent(providerMessageId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(DeliveryCommand command, String error) {
        var managed = repository.findByNotificationIdAndChannel(command.notificationId(), command.channel())
                .orElseGet(() -> DeliveryAttempt.pending(command.notificationId(), command.channel()));
        managed.markFailed(error);
        repository.save(managed);
    }
}
