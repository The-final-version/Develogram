package com.goorm.clonestagram.comment.mapper;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.dto.CommentResponse;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

public class CommentMapper {

	public static Comments toEntity(CommentRequest request, User user, Posts posts) {
		return Comments.builder()
			.users(new UserEntity(user))
			.posts(posts)
			.content(request.getContent())
			.build();
	}

	public static CommentRequest toRequest(Comments comments) {
		return new CommentRequest(comments.getUsers().getId(), comments.getPosts().getId(), comments.getContent());
	}

	public static CommentResponse toResponse(Comments comments) {
		return new CommentResponse(
			comments.getId(),
			comments.getUsers().getId(),
			comments.getUsers().getName(),
			comments.getPosts().getId(),
			comments.getContent(),
			comments.getCreatedAt());
	}
}
