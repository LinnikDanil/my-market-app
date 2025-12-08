package ru.practicum.market.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        var queryString = request.getQueryString();
        var remoteAddress = request.getRemoteAddr();
        var userAgent = request.getHeader("User-Agent");
        var contentType = request.getContentType();

        log.info("""
                \n====================== INCOMING REQUEST ======================
                Method       : {}
                URI          : {}
                Query        : {}
                Remote Addr  : {}
                User-Agent   : {}
                Content-Type : {}
                ==================================================
                """,
                request.getMethod(),
                request.getRequestURI(),
                queryString == null ? "-" : queryString,
                remoteAddress,
                userAgent == null ? "-" : userAgent,
                contentType == null ? "-" : contentType
        );

        return true; // Продолжаем обработку
    }
}
