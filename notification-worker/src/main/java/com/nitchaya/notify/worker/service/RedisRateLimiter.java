package com.nitchaya.notify.worker.service;

import com.nitchaya.notify.common.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class RedisRateLimiter {
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxPerMinute;

    public RedisRateLimiter(StringRedisTemplate redis,
                            @Value("${app.rate-limit.max-per-minute:30}") int maxPerMinute) {
        this.redis = redis;
        this.maxPerMinute = maxPerMinute;
    }

    public void check(String recipient, NotificationChannel channel) {
        String key = "notify:rate:" + channel.name() + ":" + sha256(recipient);
        Long count = redis.execute(SCRIPT, List.of(key), "60");
        if (count != null && count > maxPerMinute) {
            throw new RateLimitExceededException("Rate limit exceeded for " + channel);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }
}
