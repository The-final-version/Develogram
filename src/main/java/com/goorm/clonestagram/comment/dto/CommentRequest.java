package com.goorm.clonestagram.comment.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentRequest {
	private Long userId;
	private Long postId;
	private String content;

	public CommentRequest(Long userId, Long postId, String content) {
		this.userId = userId;
		this.postId = postId;
		this.content = content;
	}
}
