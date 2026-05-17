package com.example.hotelreservation.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

//     2. Scoped Values (Java 25 JEP 506) — Replace MDC for TraceId
//
//    Current TraceIdFilter uses ThreadLocal/MDC which breaks across virtual threads. Use Scoped Values:
//
//    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
//
//    // In filter:
//  ScopedValue.runWhere(TRACE_ID, traceId, () -> filterChain.doFilter(request, response));
//    Benefit: Immutable, GC-safe, propagates correctly through virtual threads and structured concurrency.



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
