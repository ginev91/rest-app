package org.example.main.mapper.table;

import org.example.main.dto.request.table.ReservationRequestDto;
import org.example.main.dto.response.table.TableReservationResponseDto;
import org.example.main.model.table.TableReservationEntity;
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