package org.example.main.mapper;

import org.example.main.dto.request.ReservationRequestDto;
import org.example.main.dto.response.TableReservationResponseDto;
import org.example.main.model.TableReservationEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ReservationMapperTest {

    @Test
    void toResponse_and_requestToEntity_behaviour() {
        ReservationMapper mapper = new ReservationMapper();

        assertThat(mapper.toResponse(null)).isNull();

        TableReservationEntity e = TableReservationEntity.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000123"))
                .tableId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000020"))
                .startTime(OffsetDateTime.parse("2025-01-01T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2025-01-01T11:00:00Z"))
                .status(null)
                .deleted(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        TableReservationResponseDto dto = mapper.toResponse(e);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        assertThat(dto.getTableId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(dto.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(dto.getStatus()).isNull();

        assertThat(mapper.requestToEntity(null)).isNull();

        ReservationRequestDto req = new ReservationRequestDto();
        req.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        req.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        req.setRequestedBy(UUID.fromString("00000000-0000-0000-0000-000000000030"));
        TableReservationEntity tr = mapper.requestToEntity(req);
        assertThat(tr).isNotNull();
        assertThat(tr.getTableId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(tr.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(tr.getStartTime()).isNull();
        assertThat(tr.getEndTime()).isNull();

        req.setFrom(OffsetDateTime.parse("2025-02-01T08:00:00Z"));
        req.setTo(OffsetDateTime.parse("2025-02-01T09:00:00Z"));
        TableReservationEntity tr2 = mapper.requestToEntity(req);
        assertThat(tr2).isNotNull();
        assertThat(tr2.getStartTime()).isEqualTo(req.getFrom());
        assertThat(tr2.getEndTime()).isEqualTo(req.getTo());
    }
}