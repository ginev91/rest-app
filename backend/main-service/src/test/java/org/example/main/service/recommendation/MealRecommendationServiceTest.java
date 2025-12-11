package org.example.main.service.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.dto.request.recommendation.MealRecommendationRequestDto;
import org.example.main.dto.response.recommendation.MealRecommendationResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealRecommendationServiceTest {

    @Mock
    LocalModelRecommendationService localModelService;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    ObjectMapper objectMapper;

    MealRecommendationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.lenient().when(valueOps.get(anyString())).thenReturn(null);
        
        
        Mockito.lenient().when(localModelService.generate(anyString())).thenReturn(Mono.empty());

        service = new MealRecommendationService(localModelService, objectMapper, redisTemplate);
    }

    @Test
    void recommend_returnsEmpty_whenModelReturnsBlank() {
        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("anything");

        when(localModelService.generate(anyString())).thenReturn(Mono.just("   ")); 

        List<MealRecommendationResponseDto> result = service.recommend(req).block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(localModelService).generate(anyString());
        verify(redisTemplate.opsForValue()).get(anyString());
        
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void recommend_parsesValidJsonArray_intoDtos() {
        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("low-carb");

        String json = "[{\"description\":\"Grilled Salmon\",\"menuItemName\":\"Salmon\",\"score\":0.95}]";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(json));

        List<MealRecommendationResponseDto> result = service.recommend(req).block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        MealRecommendationResponseDto dto = result.get(0);
        assertThat(dto.getDescription()).isEqualTo("Grilled Salmon");
        assertThat(dto.getMenuItemName()).isEqualTo("Salmon");

        verify(localModelService).generate(anyString());
        verify(redisTemplate.opsForValue()).get(anyString());
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void recommend_unquotesQuotedJsonString_and_parses() {
        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("quoted");

        String inner = "[{\"description\":\"R\",\"menuItemName\":\"M\",\"score\":0.5}]";
        String quoted = "\"" + inner.replace("\"", "\\\"") + "\"";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(quoted));

        List<MealRecommendationResponseDto> result = service.recommend(req).block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("R");

        verify(localModelService).generate(anyString());
        verify(redisTemplate.opsForValue()).get(anyString());
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void recommend_stripsCodeFences_and_parses() {
        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("codefence");

        String inner = "[{\"description\":\"CodeFence\",\"menuItemName\":\"CF\",\"score\":0.8}]";
        String fenced = "```json\n" + inner + "\n```";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(fenced));

        List<MealRecommendationResponseDto> result = service.recommend(req).block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMenuItemName()).isEqualTo("CF");

        verify(localModelService).generate(anyString());
        verify(redisTemplate.opsForValue()).get(anyString());
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void cachedInvalidJson_fallsBackToGenerate_andCachesNewResult() {
        
        when(valueOps.get(anyString())).thenReturn("not-a-json");

        String json = "[{\"description\":\"FromModel\",\"menuItemName\":\"M\",\"score\":0.95}]";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(json));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("anything");

        
        List<MealRecommendationResponseDto> result = service.recommend(req).block();

        
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("FromModel");

        verify(localModelService).generate(anyString());
        verify(redisTemplate.opsForValue()).get(anyString());
        
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void whenRedisMissing_serviceWorksWithoutCaching() {
        ObjectMapper om = new ObjectMapper();
        MealRecommendationService noRedisService = new MealRecommendationService(localModelService, om, null);

        String json = "[{\"description\":\"NoRedis\",\"menuItemName\":\"NR\",\"score\":0.7}]";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(json));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("anything");

        List<MealRecommendationResponseDto> result = noRedisService.recommend(req).block();

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("NoRedis");

        
        verify(localModelService).generate(anyString());
    }

    @Test
    void keyForRequest_fallback_whenObjectMapperWriteFails_usesFallbackKey() throws Exception {
        ObjectMapper spyMapper = spy(new ObjectMapper());
        
        doThrow(new RuntimeException("boom")).when(spyMapper).writeValueAsString(argThat(o -> o != null && o.getClass().getSimpleName().contains("MealRecommendationRequestDto")));

        String json = "[{\"description\":\"FromModelAfterFallback\",\"menuItemName\":\"FB\",\"score\":0.5}]";

        
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(localModelService.generate(anyString())).thenReturn(Mono.just(json));

        MealRecommendationService svc = new MealRecommendationService(localModelService, spyMapper, redisTemplate);

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("anything");

        List<MealRecommendationResponseDto> result = svc.recommend(req).block();

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("FromModelAfterFallback");

        
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).get(keyCaptor.capture());
        String usedKey = keyCaptor.getValue();
        assertThat(usedKey).startsWith("mealrec:raw:");

        
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }
}