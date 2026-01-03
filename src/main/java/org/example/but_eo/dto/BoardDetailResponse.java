package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.but_eo.entity.Board;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BoardDetailResponse {
    private String boardId;
    private String title;
    private String userHashId;
    private String content;
    private Board.State state;
    private Board.Category category;
    private Board.Event event; // 추가
    private String userName;
    private List<String> fileUrls;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;
}
