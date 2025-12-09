package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.dto.request.MealRecommendationRequestDto;
import org.example.main.dto.response.MealRecommendationResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class MealRecommendationServiceTest {

    @Mock
    LocalModelService localModelService;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    MealRecommendationService svc;

    @Test
    void recommend_returnsEmpty_whenModelReturnsBlank() {
        when(localModelService.generate(anyString())).thenReturn(Mono.just(""));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("some request");

        List<MealRecommendationResponseDto> out = svc.recommend(req)
                .block(Duration.ofSeconds(1));

        assertThat(out).isNotNull().isEmpty();
        verify(localModelService).generate(anyString());
    }

    @Test
    void recommend_parsesValidJsonArray_intoDtos() throws Exception {
        String json = "[{\"recipe\":\"MyRecipe\",\"menuItemName\":\"Grilled Fish\",\"score\":0.92}]";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(json));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("fish please");

        List<MealRecommendationResponseDto> out = svc.recommend(req).block(Duration.ofSeconds(1));

        assertThat(out).isNotNull().hasSize(1);
        MealRecommendationResponseDto dto = out.get(0);
        assertThat(dto.getMenuItemName()).isEqualTo("Grilled Fish");
        verify(localModelService).generate(anyString());
    }

    @Test
    void recommend_stripsCodeFences_and_parses() {
        String inner = "[{\"menuItemName\":\"FromFence\"}]";
        String fenced = "```json\n" + inner + "\n```";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(fenced));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("fence");

        List<MealRecommendationResponseDto> out = svc.recommend(req).block(Duration.ofSeconds(1));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getMenuItemName()).isEqualTo("FromFence");
    }

    @Test
    void recommend_unquotesQuotedJsonString_and_parses() {
        String inner = "[{\"menuItemName\":\"Quoted\"}]";
        
        String quoted = "\"" + inner.replace("\"", "\\\"") + "\"";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(quoted));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("quote");

        List<MealRecommendationResponseDto> out = svc.recommend(req).block(Duration.ofSeconds(1));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getMenuItemName()).isEqualTo("Quoted");
    }

    @Test
    void recommend_returnsEmpty_whenModelOutputInvalidJson() {
        String bad = "[this is not valid json]";
        when(localModelService.generate(anyString())).thenReturn(Mono.just(bad));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("bad");

        List<MealRecommendationResponseDto> out = svc.recommend(req).block(Duration.ofSeconds(1));
        assertThat(out).isNotNull().isEmpty();
    }

    @Test
    void recommend_passesPromptToLocalModelService() {
        when(localModelService.generate(anyString())).thenReturn(Mono.just("[]"));

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("please recommend low-carb meals");

        svc.recommend(req).block(Duration.ofSeconds(1));

        ArgumentCaptor<String> capt = ArgumentCaptor.forClass(String.class);
        verify(localModelService).generate(capt.capture());
        String usedPrompt = capt.getValue();
        assertThat(usedPrompt).contains("User request:");
        assertThat(usedPrompt).contains("please recommend low-carb meals");
    }
}