package org.example.but_eo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamJoinRequestDto {
    private String userId;
    private String userName;
    private String profileImg;
    private LocalDateTime requestedAt;
}
