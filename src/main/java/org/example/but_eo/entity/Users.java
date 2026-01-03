package org.example.but_eo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users implements UserDetails {

    @Id
    @Column(length = 64, nullable = false)
    private String userHashId; //해시256

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public enum State{
        ACTIVE, DELETED_WAIT
    }; //상태 -> 활성화, 삭제대기

    public enum Division{
        USER, ADMIN, BUSINESS
    }; //유저, 관리자, 사업자

    public enum LoginType {
        BUTEO, KAKAO, NAVER
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Division division;

    @Column(length = 20, nullable = false)
    private String name; //닉네임

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 30, nullable = true)
    private String tel;

    @Column(length = 30, nullable = true)
    private String preferSports; //선호종목

    @Column(length = 30, nullable = true)
    private String region; //지역

    @Column(nullable = true)
    private int badmintonScore;

    @Column(nullable = true)
    private int tennisScore;

    @Column(nullable = true)
    private int tableTennisScore;

    @Column(nullable = true)
    private int bowlingScore;

    @Column(nullable = true)
    private String gender; //성별 0 : 남자, 1: 여자

    @Column(nullable = true)
    private String birth;

    @Column(nullable = true)
    private String profile; //프로필 사진

    @Column(nullable = false)
    private LocalDateTime createdAt; //계정 생성일

    @Column(length = 30, nullable = true)
    private String businessNumber; // 사업자 등록번호 (사업자만 해당)

    @OneToMany(mappedBy = "user")
    private List<Board> boardList = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Comment> commentList = new ArrayList<>();

    @OneToMany(mappedBy = "senderUser")
    private List<Notification> senderList = new ArrayList<>();

    @OneToMany(mappedBy = "receiverUser")
    private List<Notification> receiverList = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ChattingMember> chatingList = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private  List<TeamMember> teamMemberList = new ArrayList<>();

    @Column(length = 500)
    private String refreshToken;  // 리프레시 토큰 저장

    @Column(nullable = true)
    private Boolean emailVerified = false;
}
