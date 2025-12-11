package org.example.main.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.dto.request.recommendation.MealRecommendationRequestDto;
import org.example.main.dto.response.recommendation.MealRecommendationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class MealRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(MealRecommendationService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final LocalModelRecommendationService localModelService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public MealRecommendationService(LocalModelRecommendationService localModelService,
                                     ObjectMapper objectMapper,
                                     @Nullable StringRedisTemplate redisTemplate) {
        this.localModelService = localModelService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public Mono<List<MealRecommendationResponseDto>> recommend(MealRecommendationRequestDto request) {
        String prompt = defaultPrompt(request);
        String cacheKey = keyForRequest(request);

        Mono<String> cachedMono;
        if (redisTemplate == null) {
            cachedMono = Mono.empty();
        } else {
            cachedMono = Mono.fromCallable(() -> redisTemplate.opsForValue().get(cacheKey))
                    .subscribeOn(Schedulers.boundedElastic());
        }

        return cachedMono
                .flatMap(cachedJson -> {
                    if (cachedJson != null && !cachedJson.isBlank()) {
                        try {
                            List<MealRecommendationResponseDto> cached = objectMapper.readValue(
                                    cachedJson, new TypeReference<List<MealRecommendationResponseDto>>() {});
                            log.debug("MealRecommendationService: cache hit for key={}, items={}", cacheKey, cached.size());
                            return Mono.just(cached);
                        } catch (Exception ex) {
                            log.warn("MealRecommendationService: failed to parse cached value, ignoring cache: {}", ex.getMessage());
                            return Mono.empty();
                        }
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(
                        localModelService.generate(prompt)
                                .map(raw -> {
                                    if (raw == null || raw.isBlank()) return List.<MealRecommendationResponseDto>of();

                                    String cleaned = stripCodeFences(raw.trim());
                                    cleaned = stripQuotes(cleaned);

                                    try {
                                        List<?> parsed = objectMapper.readValue(cleaned, List.class);
                                        List<MealRecommendationResponseDto> result = new ArrayList<>();
                                        for (Object o : parsed) {
                                            MealRecommendationResponseDto dto = objectMapper.convertValue(o, MealRecommendationResponseDto.class);
                                            result.add(dto);
                                        }
                                        log.debug("Parsed {} recommendation(s) from model output.", result.size());
                                        return result;
                                    } catch (Exception ex) {
                                        log.warn("Failed to parse model output as JSON array: {}", ex.getMessage());
                                        return List.<MealRecommendationResponseDto>of();
                                    }
                                })
                                .flatMap(resultList -> {
                                    if (redisTemplate != null && resultList != null && !resultList.isEmpty()) {
                                        return Mono.fromRunnable(() -> {
                                            try {
                                                log.debug("Saving cached recommendations for model output.");
                                                String json = objectMapper.writeValueAsString(resultList);
                                                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                                            } catch (Exception e) {
                                                log.warn("MealRecommendationService: failed to cache result: {}", e.getMessage());
                                            }
                                        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(resultList);
                                    } else {
                                        return Mono.just(resultList);
                                    }
                                })
                );
    }

    private String keyForRequest(MealRecommendationRequestDto req) {
        try {
            String payload = req == null ? "" : objectMapper.writeValueAsString(req);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return "mealrec:" + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            // fallback key if hashing fails (very unlikely)
            String fallback = "mealrec:raw:" + (req == null ? "null" : Objects.toString(req.hashCode()));
            log.warn("MealRecommendationService: fallback cache key used due to error: {}", ex.getMessage());
            return fallback;
        }
    }

    private String stripCodeFences(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int end = trimmed.lastIndexOf("```");
            if (end > 3) {
                String inner = trimmed.substring(trimmed.indexOf('\n') + 1, end).trim();
                return inner;
            }
        }
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            try {
                return objectMapper.readValue(s, String.class).trim();
            } catch (Exception ignored) {}
        }
        return s;
    }

    private String defaultPrompt(MealRecommendationRequestDto request) {
        String userPrompt = request == null || request.getPrompt() == null ? "" : request.getPrompt().trim();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful nutrition assistant. ");
        sb.append("Given the user's request below, return ONLY a JSON array of recommendation objects and nothing else. ");
        sb.append("Do NOT include any explanation, prompt echo, or surrounding text; do NOT include markdown or code fences. ");
        sb.append("Each object may include fields such as recipe, description, matchedMenuItemId, menuItemId, menuItemName, score, matchPercentage, calories, protein, fats, carbs, ingredients. ");
        sb.append("If you must make assumptions (e.g. about allergies or calorie targets), include an 'assumptions' field in the first object. ");
        sb.append("Keep the array length to at most 4. Example:\n");
        sb.append("[{ \"recipe\":\"...\",\"description\":\"...\",\"menuItemName\":\"Grilled Salmon\",\"score\":0.95,\"calories\":550,\"protein\":40,\"fats\":23,\"carbs:75 }]\n\n");
        sb.append("User request: ").append(userPrompt);
        return sb.toString();
    }
}