package org.example.but_eo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@IdClass(BoardLikeKey.class)
public class BoardLike {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_hash_id")
    private Users user;

    @Id
    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board;

    private LocalDateTime likedAt;
}
