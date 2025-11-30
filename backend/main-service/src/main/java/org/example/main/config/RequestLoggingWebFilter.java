package org.example.main.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Logs incoming request method/path and all headers at DEBUG level
 * and a single summary line at INFO level.
 */
@Component
public class RequestLoggingWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        log.info("Incoming request: {} {} origin={} AuthorizationPresent={}", method, path, origin, auth != null);
        if (log.isDebugEnabled()) {
            exchange.getRequest().getHeaders().forEach((k, v) -> log.debug("Header: {} = {}", k, v));
        }
        return chain.filter(exchange);
    }
}