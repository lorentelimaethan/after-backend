package com.afterApp.after.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;


@Component
public class RequestTimingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.nanoTime() - start;

            log.info(
                    "{} {} -> {} took {} ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration / 1_000_000.0
            );
        }
    }
}
