package com.shhdoc.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 traceId를 MDC에 심어 같은 요청에서 나온 로그 줄을 묶어 추적할 수 있게 하고,
 * 요청이 끝나면 METHOD/URI/STATUS/처리시간 한 줄을 access log로 남긴다.
 * 클라이언트/프록시가 {@code X-Request-Id}를 이미 보냈으면 그대로 쓰고, 없으면 새로 발급해
 * 응답 헤더로도 돌려준다 — 문의 들어왔을 때 그 값으로 로그를 바로 찾을 수 있게.
 *
 * <p>Spring Security 필터체인보다 먼저 돌아야 인증 실패(401/403)도 access log에 남고
 * traceId로 묶이므로 {@link Ordered#HIGHEST_PRECEDENCE}로 최우선 순위를 준다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Request-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);

        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), durationMs);
            MDC.remove(MDC_KEY);
        }
    }
}
