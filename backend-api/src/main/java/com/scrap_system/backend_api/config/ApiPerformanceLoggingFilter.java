package com.scrap_system.backend_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiPerformanceLoggingFilter extends OncePerRequestFilter {

    private static final Pattern VEHICLE_DETAIL_PATTERN = Pattern.compile("^/api/vehicles/\\d+$");
    private static final Pattern ADMIN_VEHICLE_DETAIL_PATTERN = Pattern.compile("^/api/admin/vehicles/\\d+$");
    private static final Pattern SAME_SERIES_PATTERN = Pattern.compile("^/api/vehicles/\\d+/same-series$");
    private static final Pattern VALUATION_PATTERN = Pattern.compile("^/api/valuation/\\d+$");

    private final PerformanceLogSupport performanceLogSupport;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !performanceLogSupport.isEnabled() || !isTargetPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean forceTrace = isForceTraceRequested(request);
        PerformanceTraceContext.setForceTrace(forceTrace);
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = performanceLogSupport.elapsedMillis(startNanos);
            String requestTarget = buildRequestTarget(request);
            String authDetail = buildAuthDetail();
            performanceLogSupport.logRequest(
                    log,
                    request.getMethod() + " " + requestTarget,
                    elapsedMs,
                    "status=" + response.getStatus() + authDetail
            );
            PerformanceTraceContext.clear();
        }
    }

    private static boolean isTargetPath(String uri) {
        return VEHICLE_DETAIL_PATTERN.matcher(uri).matches()
                || ADMIN_VEHICLE_DETAIL_PATTERN.matcher(uri).matches()
                || SAME_SERIES_PATTERN.matcher(uri).matches()
                || VALUATION_PATTERN.matcher(uri).matches();
    }

    private static boolean isForceTraceRequested(HttpServletRequest request) {
        String header = request.getHeader("X-Perf-Trace");
        if (isTruthy(header)) {
            return true;
        }
        return isTruthy(request.getParameter("tracePerformance"));
    }

    private static boolean isTruthy(String raw) {
        if (raw == null) {
            return false;
        }
        return "1".equals(raw) || "true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw);
    }

    private static String buildRequestTarget(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + query;
    }

    private static String buildAuthDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "";
        }
        return ", principal=" + authentication.getPrincipal();
    }
}
