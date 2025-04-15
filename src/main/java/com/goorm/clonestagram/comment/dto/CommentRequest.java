package com.goorm.clonestagram.comment.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentRequest {
    private Long userId;
    private Long postId;
    private String content;

    @JsonCreator
    public CommentRequest(
        @JsonProperty("userId") Long userId,
        @JsonProperty("postId") Long postId,
        @JsonProperty("content") String content) {
        this.userId = userId;
        this.postId = postId;
        this.content = content;
    }
}
