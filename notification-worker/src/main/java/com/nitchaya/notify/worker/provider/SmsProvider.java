package com.nitchaya.notify.worker.provider;

import com.nitchaya.notify.common.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class SmsProvider extends AbstractLoggingProvider {
    @Override public NotificationChannel channel() { return NotificationChannel.SMS; }
}
