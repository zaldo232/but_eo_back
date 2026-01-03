package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.but_eo.entity.Board;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardAdminResponse {
    private String boardId;
    private String title;
    private String userName;  // DTO 필드명은 유지 가능

    private String category;
    private String event;
    private LocalDateTime createdAt;
    private String state;

    public static BoardAdminResponse from(Board board) {
        return BoardAdminResponse.builder()
                .boardId(board.getBoardId())
                .title(board.getTitle())
                .userName(board.getUser().getName())  // Users.name 필드 사용
                .category(board.getCategory().toString())
                .event(board.getEvent().toString())
                .createdAt(board.getCreatedAt())
                .state(board.getState().toString())
                .build();
    }
}