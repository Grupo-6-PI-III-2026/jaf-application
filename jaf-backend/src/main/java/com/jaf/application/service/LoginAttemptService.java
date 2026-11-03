package com.jaf.application.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean bloqueado(String ip, String email) {
        return falhas(ip + "|" + email) >= MAX_FAILURES || falhas(email) >= MAX_FAILURES;
    }

    public void registrarFalha(String ip, String email) {
        incrementar(ip + "|" + email);
        incrementar(email);
    }

    public void limpar(String ip, String email) {
        attempts.remove(ip + "|" + email);
        attempts.remove(email);
    }

    private int falhas(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.expiraEm.isBefore(Instant.now())) {
            attempts.remove(key, attempt);
            return 0;
        }
        return attempt.count.get();
    }

    private void incrementar(String key) {
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.expiraEm.isBefore(Instant.now())) {
                return new Attempt(new AtomicInteger(1), Instant.now().plus(WINDOW));
            }
            current.count.incrementAndGet();
            return current;
        });
    }

    private record Attempt(AtomicInteger count, Instant expiraEm) {}
}
