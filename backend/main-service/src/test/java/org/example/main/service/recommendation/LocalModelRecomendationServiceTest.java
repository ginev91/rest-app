package org.example.main.service.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.exception.LocalModelException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocalModelService covering:
 *  - mock backend output
 *  - http backend happy path, empty response, and error mapping
 *  - normalization behavior exercised via HTTP responses
 */
class LocalModelRecomendationServiceTest {

    private WebClient.Builder mockBuilderReturning(WebClient webClient) {
        WebClient.Builder b = mock(WebClient.Builder.class);
        when(b.build()).thenReturn(webClient);
        return b;
    }

    @Test
    void generate_mock_backend_returnsJsonArrayString() {
        ObjectMapper om = new ObjectMapper();
        
        LocalModelRecommendationService svc = new LocalModelRecommendationService(
                "mock",
                "unused",
                "ollama",
                1000,
                mock(WebClient.Builder.class),
                "http://localhost:11434/api/generate",
                1000,
                om);

        String out = svc.generate("anything").block(Duration.ofSeconds(1));
        assertThat(out).isNotNull();
        assertThat(out).startsWith("[");
        assertThat(out).contains("menuItemName");
        assertThat(out).contains("Simple Grilled Chicken Salad");
    }

    @Test
    void generate_http_backend_concatenatesChunks_and_normalizes_quotedArray() {
        ObjectMapper om = new ObjectMapper();

        
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        
        String quotedArray = "\"[{\\\"menuItemName\\\":\\\"X\\\"}]\"";
        Map<String, Object> chunk = Map.of("response", quotedArray);
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.just(chunk));

        WebClient.Builder builder = mockBuilderReturning(webClient);

        LocalModelRecommendationService svc = new LocalModelRecommendationService(
                "http",
                "m",
                "cli",
                1000,
                builder,
                "http://localhost:11434/api/generate",
                1000,
                om);

        String out = svc.generate("prompt").block(Duration.ofSeconds(1));
        
        assertThat(out).isEqualTo("[{\"menuItemName\":\"X\"}]");
    }

    @Test
    void generate_http_backend_noChunks_returnsDefaultMessage() {
        ObjectMapper om = new ObjectMapper();

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.empty());

        WebClient.Builder builder = mockBuilderReturning(webClient);

        LocalModelRecommendationService svc = new LocalModelRecommendationService(
                "http",
                "m",
                "cli",
                1000,
                builder,
                "http://localhost:11434/api/generate",
                1000,
                om);

        String out = svc.generate("prompt").block(Duration.ofSeconds(1));
        
        
        assertThat(out).isEqualTo("");
    }

    @Test
    void generate_http_backend_error_isWrappedAsLocalModelException() {
        ObjectMapper om = new ObjectMapper();

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.error(new RuntimeException("boom")));

        WebClient.Builder builder = mockBuilderReturning(webClient);

        LocalModelRecommendationService svc = new LocalModelRecommendationService(
                "http",
                "m",
                "cli",
                1000,
                builder,
                "http://localhost:11434/api/generate",
                1000,
                om);

        assertThatThrownBy(() -> svc.generate("p").block(Duration.ofSeconds(1)))
                .isInstanceOf(LocalModelException.class)
                .hasMessageContaining("Local model HTTP backend failed");
    }

    @Test
    void normalizeOutput_extractsInnerJsonTextObjects_and_arrays() {
        ObjectMapper om = new ObjectMapper();

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        
        String objWithText = "{\"text\":\"[{\\\"menuItemName\\\":\\\"FromText\\\"}]\"}";
        Map<String, Object> chunk = Map.of("response", objWithText);
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.just(chunk));

        WebClient.Builder builder = mockBuilderReturning(webClient);

        LocalModelRecommendationService svc = new LocalModelRecommendationService(
                "http",
                "m",
                "cli",
                1000,
                builder,
                "http://localhost:11434/api/generate",
                1000,
                om);

        String out = svc.generate("p").block(Duration.ofSeconds(1));
        
        assertThat(out).isEqualTo("[{\"menuItemName\":\"FromText\"}]");
    }
}