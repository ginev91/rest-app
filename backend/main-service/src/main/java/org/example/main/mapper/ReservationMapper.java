package org.example.main.mapper;

import org.example.main.dto.request.ReservationRequestDto;
import org.example.main.dto.response.TableReservationResponseDto;
import org.example.main.model.enums.ReservationStatus;
import org.example.main.model.TableReservationEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
public class ReservationMapper {

    public TableReservationResponseDto toResponse(TableReservationEntity entity) {
        if (entity == null) return null;

        return TableReservationResponseDto.builder()
                .id(entity.getId())
                .tableId(entity.getTableId())
                .userId(entity.getUserId())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .deleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }


    public TableReservationEntity requestToEntity(ReservationRequestDto req) {
        if (req == null) return null;

        // Use builder so defaults from the entity class (@Builder.Default or @PrePersist) are preserved where appropriate.
        TableReservationEntity tr = TableReservationEntity.builder()
                .tableId(req.getTableId())
                .userId(req.getUserId())
                .startTime(req.getFrom())
                .endTime(req.getTo())
                .createdBy(req.getRequestedBy())
                .build();

        if (Objects.isNull(tr.getStartTime()) || Objects.isNull(tr.getEndTime())) {
            return tr;
        }

        return tr;
    }
}