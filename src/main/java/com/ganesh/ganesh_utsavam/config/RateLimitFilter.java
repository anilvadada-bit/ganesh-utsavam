package com.ganesh.ganesh_utsavam.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, RequestCounter> requestCounters =
            new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only protect payment POST endpoints
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !isPaymentEndpoint(path)) {

            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        RequestCounter counter = requestCounters.compute(
                clientIp,
                (key, existing) -> {

                    long now = Instant.now().getEpochSecond();

                    if (existing == null
                            || now - existing.windowStart
                            >= WINDOW_SECONDS) {

                        return new RequestCounter(
                                now,
                                1
                        );
                    }

                    existing.count++;
                    return existing;
                });

        if (counter.count > MAX_REQUESTS) {

            response.setStatus(429);

           

            response.setContentType("application/json");

            response.getWriter().write(
                    """
                    {
                        "success": false,
                        "message": "Too many requests. Please try again later."
                    }
                    """
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPaymentEndpoint(String path) {

        return "/api/payments/create-order".equals(path)
                || "/api/payments/verify".equals(path);
    }

    private String getClientIp(HttpServletRequest request) {

        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private static class RequestCounter {

        private final long windowStart;
        private int count;

        private RequestCounter(
                long windowStart,
                int count) {

            this.windowStart = windowStart;
            this.count = count;
        }
    }
}