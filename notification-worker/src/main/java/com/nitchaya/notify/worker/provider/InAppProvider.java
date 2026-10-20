package com.nitchaya.notify.worker.provider;

import com.nitchaya.notify.common.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppProvider extends AbstractLoggingProvider {
    @Override public NotificationChannel channel() { return NotificationChannel.IN_APP; }
}
