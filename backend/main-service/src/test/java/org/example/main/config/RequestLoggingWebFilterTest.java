package org.example.main.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingWebFilterTest {

    @Mock
    ServerWebExchange exchange;

    @Mock
    ServerHttpRequest request;

    @Mock
    WebFilterChain chain;

    @Test
    void filter_delegatesToChain_and_handlesHeaders() {
        RequestLoggingWebFilter filter = new RequestLoggingWebFilter();

        when(exchange.getRequest()).thenReturn(request);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/api/test/path"));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Origin", "http://example.com");
        headers.add("Authorization", "Bearer tok");
        when(request.getHeaders()).thenReturn(headers);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Execute and block to ensure the chain is invoked
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_handlesMissingHeaders() {
        RequestLoggingWebFilter filter = new RequestLoggingWebFilter();

        when(exchange.getRequest()).thenReturn(request);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("/"));
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }
}