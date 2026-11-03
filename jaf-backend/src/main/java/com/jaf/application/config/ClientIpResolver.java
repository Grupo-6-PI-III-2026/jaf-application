package com.jaf.application.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    private final int trustedProxyHops;

    public ClientIpResolver(@Value("${app.security.trusted-proxy-hops:1}") int trustedProxyHops) {
        this.trustedProxyHops = Math.max(0, trustedProxyHops);
    }

    public String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return request.getRemoteAddr();
        String[] addresses = forwarded.split(",");
        int index = addresses.length - 1 - trustedProxyHops;
        return index >= 0 ? addresses[index].trim() : request.getRemoteAddr();
    }
}
