package org.example.but_eo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Comment {

    @Id
    @Column(length = 64, nullable = false)
    private String commentId;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne
    @JoinColumn(name = "user_hash_id", nullable = false)
    private Users user;

    public enum State{
        PUBLIC, PRIVATE, DELETE
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = true)
    private LocalDateTime changeAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
