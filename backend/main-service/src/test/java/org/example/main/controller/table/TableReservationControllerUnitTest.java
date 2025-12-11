package org.example.main.controller.table;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.main.dto.request.table.CancelReservationRequestDto;
import org.example.main.dto.request.table.ReservationRequestDto;
import org.example.main.dto.response.table.TableReservationResponseDto;
import org.example.main.mapper.table.ReservationMapper;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.service.table.ITableReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
class TableReservationControllerUnitTest {

    @Mock
    private ITableReservationService reservationService;

    @Mock
    private ReservationMapper mapper;

    private MockMvc mvc;
    private ObjectMapper objectMapper;
    private TableReservationController controller;

    @BeforeEach
    void setUp() {
        controller = new TableReservationController(reservationService, mapper);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createReservation_returnsCreatedDto() throws Exception {
        UUID tableId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);

        ReservationRequestDto req = ReservationRequestDto.builder()
                .tableId(tableId)
                .from(from)
                .to(to)
                .requestedBy(requestedBy)
                .userId(userId)
                .build();

        TableReservationEntity createdEntity = new TableReservationEntity();
        createdEntity.setId(UUID.randomUUID());
        createdEntity.setTableId(tableId);
        createdEntity.setUserId(userId);
        createdEntity.setStartTime(from);
        createdEntity.setEndTime(to);

        TableReservationResponseDto resp = TableReservationResponseDto.builder()
                .id(createdEntity.getId())
                .tableId(tableId)
                .userId(userId)
                .startTime(from)
                .endTime(to)
                .status(createdEntity.getStatus() != null ? createdEntity.getStatus().name() : null)
                .build();

        when(reservationService.reserveTable(
                any(UUID.class),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(UUID.class),
                any(UUID.class)
        )).thenReturn(createdEntity);

        when(mapper.toResponse(createdEntity)).thenReturn(resp);

        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdEntity.getId().toString()))
                .andExpect(jsonPath("$.tableId").value(tableId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void cancel_returnsCancelledDto() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID cancelledBy = UUID.randomUUID();

        CancelReservationRequestDto cancelReq = CancelReservationRequestDto.builder()
                .cancelledBy(cancelledBy)
                .build();

        TableReservationEntity cancelledEntity = new TableReservationEntity();
        cancelledEntity.setId(reservationId);
        cancelledEntity.setDeleted(true);

        TableReservationResponseDto dto = TableReservationResponseDto.builder()
                .id(reservationId)
                .status("CANCELLED")
                .build();

        when(reservationService.cancelReservation(eq(reservationId), any(UUID.class))).thenReturn(cancelledEntity);
        when(mapper.toResponse(cancelledEntity)).thenReturn(dto);

        mvc.perform(post("/api/reservations/{id}/cancel", reservationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getActiveForTable_returnsListOfDtos() throws Exception {
        UUID tableId = UUID.randomUUID();

        TableReservationEntity e = new TableReservationEntity();
        e.setId(UUID.randomUUID());
        e.setTableId(tableId);

        TableReservationResponseDto dto = TableReservationResponseDto.builder()
                .id(e.getId())
                .tableId(tableId)
                .build();

        when(reservationService.findActiveReservationsForTable(eq(tableId))).thenReturn(List.of(e));
        when(mapper.toResponse(e)).thenReturn(dto);

        mvc.perform(get("/api/reservations/table/{tableId}", tableId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(e.getId().toString()))
                .andExpect(jsonPath("$[0].tableId").value(tableId.toString()));
    }

    @Test
    void getHistoryForTable_returnsListOfDtos() throws Exception {
        UUID tableId = UUID.randomUUID();

        TableReservationEntity e = new TableReservationEntity();
        e.setId(UUID.randomUUID());
        e.setTableId(tableId);

        TableReservationResponseDto dto = TableReservationResponseDto.builder()
                .id(e.getId())
                .tableId(tableId)
                .build();

        when(reservationService.findReservationHistoryForTable(eq(tableId))).thenReturn(List.of(e));
        when(mapper.toResponse(e)).thenReturn(dto);

        mvc.perform(get("/api/reservations/table/{tableId}/history", tableId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(e.getId().toString()))
                .andExpect(jsonPath("$[0].tableId").value(tableId.toString()));
    }
}