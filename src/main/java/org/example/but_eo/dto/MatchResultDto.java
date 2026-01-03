package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.but_eo.entity.Matching;

@Getter
@Setter
@AllArgsConstructor
public class MatchResultDto {
    private String matchId;
    private Matching.State status;
    private String opponentTeamName;
}
