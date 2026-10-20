package com.nitchaya.notify.worker.provider;

import com.nitchaya.notify.common.DeliveryCommand;
import com.nitchaya.notify.common.NotificationChannel;

public interface DeliveryProvider {
    NotificationChannel channel();
    String send(DeliveryCommand command);
}
