package ru.practicum.market.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.InetSocketAddress;

@Component
@Slf4j
public class RouteLoggingFilter {

    public HandlerFilterFunction<ServerResponse, ServerResponse> logging() {
        return (request, next) -> {
            var query = request.uri().getQuery();
            var remoteAddress = request.remoteAddress().map(InetSocketAddress::toString).orElse("-");
            var userAgent = request.headers().firstHeader("User-Agent");
            var contentType = request.headers().contentType().map(MimeType::toString).orElse("-");

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
                    request.method(),
                    request.path(),
                    query == null ? "-" : query,
                    remoteAddress,
                    userAgent == null ? "-" : userAgent,
                    contentType
            );

            return next.handle(request);
        };
    }
}
