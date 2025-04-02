package com.goorm.clonestagram.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private long id;
    private Long userId;
    private String username;
    private Long postId;
    private String content;
    private LocalDateTime createdAt;
}
