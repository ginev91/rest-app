package org.example.main.integration;

import org.example.main.config.TestSecurityConfig;
import org.example.main.dto.request.ReservationRequestDto;
import org.example.main.dto.response.TableReservationResponseDto;
import org.example.main.mapper.ReservationMapper;
import org.example.main.model.TableReservationEntity;
import org.example.main.model.enums.ReservationStatus;
import org.example.main.repository.TableReservationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.main.allow-bean-definition-overriding=true",
                      "jwt.secret=0123456789abcdefghijklmnopqrstuv"})
@ActiveProfiles("test")
@Import({TableReservationApiE2ETest.TestConfig.class, TestSecurityConfig.class})
class TableReservationApiE2ETest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ReservationMapper reservationMapper() {
            return Mockito.mock(ReservationMapper.class);
        }
    }

    @Autowired
    private org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

    @Autowired
    private TableReservationRepository reservationRepository;

    @Autowired
    private ReservationMapper mapper; 

    @Test
    @Transactional
    void create_list_and_cancel_end_to_end_without_auth() {
        when(mapper.toResponse(ArgumentMatchers.any(TableReservationEntity.class)))
                .thenAnswer(invocation -> {
                    TableReservationEntity e = invocation.getArgument(0);
                    return TableReservationResponseDto.builder()
                            .id(e.getId())
                            .tableId(e.getTableId())
                            .userId(e.getUserId())
                            .startTime(e.getStartTime())
                            .endTime(e.getEndTime())
                            .status(e.getStatus() != null ? e.getStatus().name() : null)
                            .build();
                });

        reservationRepository.deleteAll();

        UUID tableId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusDays(1);
        OffsetDateTime to = from.plusHours(2);

        ReservationRequestDto req = ReservationRequestDto.builder()
                .tableId(tableId)
                .from(from)
                .to(to)
                .requestedBy(requestedBy)
                .userId(userId)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<TableReservationResponseDto> createResp =
                restTemplate.postForEntity("/api/reservations", new HttpEntity<>(req, headers), TableReservationResponseDto.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        TableReservationResponseDto created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getTableId()).isEqualTo(tableId);
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.ACTIVE.name());

        
        ResponseEntity<List<TableReservationResponseDto>> listResp = restTemplate.exchange(
                "/api/reservations/table/" + tableId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}