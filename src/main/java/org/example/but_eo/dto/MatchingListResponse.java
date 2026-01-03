package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.but_eo.entity.Matching;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingListResponse {
    private String matchId;
    private String matchRegion;
    private String teamName;
    private String teamImg;
    private String teamRegion;
    private int teamRating;
    private String stadiumName;
    private LocalDateTime matchDate;
    private String matchType;
    private Boolean loan;
    private ChallengerTeamResponse challengerTeam;
    private List<ChallengerTeamResponse> challengerTeams;

    @Override
    public String toString() {
        return "MatchingListResponse{" +
                "matchId='" + matchId + '\'' +
                ", matchRegion='" + matchRegion + '\'' +
                ", teamName='" + teamName + '\'' +
                ", teamImg='" + teamImg + '\'' +
                ", teamRegion='" + teamRegion + '\'' +
                ", teamRating=" + teamRating +
                ", stadiumName='" + stadiumName + '\'' +
                ", matchDate=" + matchDate +
                ", matchType='" + matchType + '\'' +
                ", loan=" + loan +
                ", challengerTeam=" + challengerTeam +
                ", challengerTeams=" + challengerTeams +
                '}';
    }

}

