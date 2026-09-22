package com.travelmanagementsystem.shared.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int maxAttempts;
    private final long windowMs;
    private final java.util.concurrent.ConcurrentHashMap<String, RequestCounter> counters = new java.util.concurrent.ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${rate-limit.login.max-attempts}") int maxAttempts,
            @Value("${rate-limit.login.window-ms}") long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (isLoginRequest(request)) {
            String clientIp = getClientIp(request);
            RequestCounter counter = counters.compute(clientIp, (key, existing) -> {
                if (existing == null || System.currentTimeMillis() - existing.windowStart > windowMs) {
                    return new RequestCounter(System.currentTimeMillis());
                }
                return existing;
            });

            if (counter.incrementAndGet() > maxAttempts) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);

                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {"code":"RATE_LIMIT_EXCEEDED","message":"Too many login attempts. Please try again later."}""");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "/api/auth/login".equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void resetCounters() {
        counters.clear();
    }

    private static class RequestCounter {
        final long windowStart;
        private final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
