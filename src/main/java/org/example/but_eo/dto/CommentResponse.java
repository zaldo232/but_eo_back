package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String commentId;
    private String userHashId;
    private String userName;
    private String content;
    private LocalDateTime createdAt;
    private int likeCount;
    private String profileImg;
}