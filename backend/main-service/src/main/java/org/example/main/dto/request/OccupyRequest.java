package org.example.main.dto.request;

/**
 * Request DTO used to mark a table occupied for a duration (minutes).
 * Public fields kept for simple binding from JSON.
 */
public class OccupyRequest {

    public Integer tableNumber;
    public int minutes;

    public OccupyRequest() {}
}