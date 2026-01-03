package org.example.but_eo.dto;

import lombok.Data;

@Data
public class MatchResponseDto {
    private MatchResponseType response;

    public enum MatchResponseType {
        ACCEPTED, REJECTED
    }
}
