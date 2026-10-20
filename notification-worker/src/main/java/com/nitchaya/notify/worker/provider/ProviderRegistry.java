package com.nitchaya.notify.worker.provider;

import com.nitchaya.notify.common.NotificationChannel;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderRegistry {
    private final Map<NotificationChannel, DeliveryProvider> providers = new EnumMap<>(NotificationChannel.class);

    public ProviderRegistry(List<DeliveryProvider> deliveryProviders) {
        deliveryProviders.forEach(provider -> providers.put(provider.channel(), provider));
    }

    public DeliveryProvider get(NotificationChannel channel) {
        var provider = providers.get(channel);
        if (provider == null) throw new IllegalArgumentException("No provider configured for " + channel);
        return provider;
    }
}
