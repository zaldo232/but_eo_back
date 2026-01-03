package org.example.but_eo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.but_eo.entity.Team;
import org.example.but_eo.entity.Team.Event;
import org.example.but_eo.entity.Team.Team_Case;
import org.example.but_eo.entity.Team.Team_Type;
import org.example.but_eo.entity.TeamMember;

import java.util.List;

@Getter
@Setter
@Builder
public class TeamResponse {
    private String teamId;
    private String teamName;
    private String region;
    private int memberAge;
    private int rating;
    private String teamDescription;
    private String teamImg;
    private Event event;
    private Team_Type teamType;
    private Team_Case teamCase;

    private String myJoinStatus; // "MEMBER", "PENDING", "NONE"

    private int totalMembers;
    private int matchCount;
    private int winCount;
    private int loseCount;
    private int drawCount;
    private int totalReview;

    private double avgReviewRating;

    private List<MemberDto> members;
    private List<String> matchIds;

    @Getter
    @Builder
    public static class MemberDto {
        private String name;
        private boolean isLeader;
    }

    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .region(team.getRegion())
                .memberAge(team.getMemberAge())
                .rating(team.getRating())
                .teamDescription(team.getTeamDescription())
                .teamImg(team.getTeamImg())
                .event(team.getEvent())
                .teamType(team.getTeamType())
                .teamCase(team.getTeamCase())

                .totalMembers(team.getTeamMemberList() != null ? team.getTeamMemberList().size() : 0)
                .matchCount(team.getMatchCount())
                .winCount(team.getWinCount())
                .loseCount(team.getLoseCount())
                .drawCount(team.getDrawCount())
                .totalReview(team.getTotalReview())

                .members(team.getTeamMemberList().stream()
                        .map(tm -> MemberDto.builder()
                                .name(tm.getUser().getName())
                                .isLeader(tm.getType() == TeamMember.Type.LEADER)
                                .build())
                        .toList())
                .matchIds(team.getMatchingList().stream()
                        .map(m -> m.getMatchId())
                        .toList())
                .build();
    }

}