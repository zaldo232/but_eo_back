package org.example.but_eo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chatting {

    @Id
    @Column( length = 64, nullable = false)
    private String chatId;

    public enum State {
        PUBLIC, PRIVATE, DELETE
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "chatting")
    private List<ChattingMember> memberList = new ArrayList<>();
    
    //TODO: 채팅 메세지 테이블 연결
}
