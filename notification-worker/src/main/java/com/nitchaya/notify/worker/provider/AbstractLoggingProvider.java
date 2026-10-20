package com.nitchaya.notify.worker.provider;

import com.nitchaya.notify.common.DeliveryCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public abstract class AbstractLoggingProvider implements DeliveryProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public String send(DeliveryCommand command) {
        if (command.recipient().toLowerCase().contains("fail")) {
            throw new TransientDeliveryException("Simulated provider outage for demo recipient: " + command.recipient());
        }
        String providerId = channel().name().toLowerCase() + "-" + UUID.randomUUID();
        log.info("Delivered notification={} channel={} recipient={} providerMessageId={}",
                command.notificationId(), channel(), command.recipient(), providerId);
        return providerId;
    }
}
