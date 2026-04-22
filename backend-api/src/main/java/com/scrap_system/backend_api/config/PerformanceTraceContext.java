package com.scrap_system.backend_api.config;

public final class PerformanceTraceContext {

    private static final ThreadLocal<Boolean> FORCE_TRACE = ThreadLocal.withInitial(() -> false);

    private PerformanceTraceContext() {
    }

    public static void setForceTrace(boolean forceTrace) {
        FORCE_TRACE.set(forceTrace);
    }

    public static boolean isForceTrace() {
        return Boolean.TRUE.equals(FORCE_TRACE.get());
    }

    public static void clear() {
        FORCE_TRACE.remove();
    }
}
