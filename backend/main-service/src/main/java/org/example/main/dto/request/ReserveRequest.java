package org.example.main.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request DTO used to reserve a table for a time window.
 * Public fields kept to match controller binding behavior used elsewhere.
 */
public class ReserveRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public OffsetDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public OffsetDateTime to;

    public UUID userId;

    public ReserveRequest() {}
}