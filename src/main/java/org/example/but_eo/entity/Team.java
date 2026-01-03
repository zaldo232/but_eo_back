package org.example.but_eo.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Team {

    @Id
    @Column(length = 64, nullable = false)
    private String teamId;

    public enum Team_Type {
        SOLO("개인"),
        TEAM("팀");

        private final String displayName;

        Team_Type(String displayName) {
            this.displayName = displayName;
        }

        @JsonValue
        public String getDisplayName() {
            return displayName;
        }

        @JsonCreator
        public static Team_Type from(String value) {
            for (Team_Type t : Team_Type.values()) {
                if (t.name().equalsIgnoreCase(value) || t.displayName.equals(value)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Invalid team_type: " + value);
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team_Type teamType;

    @Column(nullable = true)
    private String teamImg; //팀 프로필 사진

    @Column(length = 20, nullable = false, unique = true)
    private String teamName;

    @Column(length = 30, nullable = false)
    private String region;

    @Column(nullable = false)
    private int memberAge; // 팀 평균 연령

    @Column(nullable = false)
    private int rating; // 팀 점수

    public enum Team_Case {
        TEENAGER("청소년"),
        UNIVERSITY("대학생"),
        OFFICE("직장인"),
        CLUB("동호회"),
        FEMALE("여성"),
        ETC("기타");

        private final String displayName;

        Team_Case(String displayName) {
            this.displayName = displayName;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getDisplayName() {
            return displayName;
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static Team_Case from(String value) {
            for (Team_Case t : Team_Case.values()) {
                if (t.name().equalsIgnoreCase(value) || t.displayName.equals(value)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Invalid team_case: " + value);
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Team_Case teamCase;

    @Column(nullable = false)
    private int totalMembers;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String teamDescription; //팀 설명

    @Column(nullable = true)
    private int totalReview; //리뷰 총합점

    public enum Event {
        SOCCER("축구"),
        BASKETBALL("농구"),
        BASEBALL("야구"),
        TENNIS("테니스"),
        TABLE_TENNIS("탁구"),
        BADMINTON("배드민턴"),
        FUTSAL("풋살"),
        BOWLING("볼링");

        private final String displayName;

        Event(String displayName) {
            this.displayName = displayName;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getDisplayName() {
            return displayName;
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static Event from(String value) {
            for (Event e : Event.values()) {
                if (e.name().equalsIgnoreCase(value) || e.displayName.equals(value)) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Invalid event: " + value);
        }
    } //축구, 풋살, 야구, 농구, 배드민턴, 테니스, 탁구, 볼링

    public enum State{
        ACTIVE, DELETED
    };

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state = State.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Event event;

    @Column(nullable = false)
    private int matchCount;

    @Column(nullable = false)
    private int winCount;

    @Column(nullable = false)
    private int loseCount;

    @Column(nullable = false)
    private int drawCount;

    @OneToMany(mappedBy = "team")
    private List<ChallengerList> challengerList = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    private List<TeamMember> teamMemberList = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    private List<Matching> matchingList = new ArrayList<>();

    @OneToMany(mappedBy = "challengerTeam")
    private List<Matching> challengerTeamList = new ArrayList<>();

    @OneToMany(mappedBy = "winnerTeam")
    private List<Matching> winnerTeamList = new ArrayList<>();

    @OneToMany(mappedBy = "loserTeam")
    private List<Matching> loserTeamList = new ArrayList<>();
}
