package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponseDto {
    private String userHashId;
    private String name;
    private String email;
    private String tel;
    private String region;
    private String division;
    private String preferSports;
    private String gender;
    private String profile;
    private String birth;
    private int badmintonScore;
    private int tennisScore;
    private int tableTennisScore;
    private int bowlingScore;
    private LocalDateTime createdAt;

}
