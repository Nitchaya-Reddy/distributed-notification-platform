package com.nitchaya.notify.worker.provider;

import com.nitchaya.notify.common.NotificationChannel;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProviderRegistryTest {
    @Test
    void selectsProviderByChannel() {
        var email = new EmailProvider();
        var sms = new SmsProvider();
        var registry = new ProviderRegistry(List.of(email, sms));
        assertThat(registry.get(NotificationChannel.EMAIL)).isSameAs(email);
        assertThat(registry.get(NotificationChannel.SMS)).isSameAs(sms);
    }
}
