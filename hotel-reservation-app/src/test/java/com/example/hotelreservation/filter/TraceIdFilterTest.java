package com.example.hotelreservation.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {

    @InjectMocks
    private TraceIdFilter traceIdFilter;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_usesTraceIdFromRequestHeader() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "existing-trace-id");

        traceIdFilter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("existing-trace-id");
    }

    @Test
    void doFilterInternal_generatesTraceIdWhenHeaderAbsent() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        traceIdFilter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER))
                .isNotNull()
                .isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void doFilterInternal_generatesTraceIdWhenHeaderIsBlank() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "   ");

        traceIdFilter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER))
                .isNotNull()
                .isNotBlank()
                .isNotEqualTo("   ")
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    }

    @Test
    void doFilterInternal_putsTraceIdInMdcDuringFilterChain() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "mdc-trace-id");

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isEqualTo("mdc-trace-id");
            return null;
        }).when(filterChain).doFilter(any(), any());

        traceIdFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_removesMdcTraceIdAfterFilterChain() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        traceIdFilter.doFilter(request, response, filterChain);

        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_delegatesToFilterChain() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        traceIdFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}