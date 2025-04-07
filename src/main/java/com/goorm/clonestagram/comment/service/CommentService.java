package com.goorm.clonestagram.comment.service;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.mapper.CommentMapper;
import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.exception.UnauthorizedCommentAccessException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {

	private final CommentRepository commentRepository;

	private final UserExternalQueryService userService;
	private final PostService postService;

	@Transactional
	public Comments createComment(CommentRequest commentRequest) {
		Comments comment = CommentMapper.toEntity(commentRequest,
			userService.findByIdAndDeletedIsFalse(commentRequest.getUserId()),
			postService.findByIdAndDeletedIsFalse(commentRequest.getPostId()));

		return commentRepository.save(comment);
	}

	// @Transactional
	// public Comments createCommentWithRollback(CommentRequest commentRequest) {
	// 	Users users = userService.findByIdAndDeletedIsFalse(commentRequest.getUserId());
	// 	Posts posts = postService.findByIdAndDeletedIsFalse(commentRequest.getPostId());
	//
	// 	return commentRepository.save(CommentMapper.toEntity(commentRequest, users, posts));
	// }

	@Transactional(readOnly = true)
	public Comments getCommentById(Long id) {
		return commentRepository.findById(id)
			.orElseThrow(() -> new CommentNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public List<Comments> getCommentsByPostId(Long postId) {
		if (!postService.existsByIdAndDeletedIsFalse(postId)) {
			throw new PostNotFoundException(postId);
		}

		List<Comments> comments = commentRepository.findByPosts_Id(postId);

		if (comments.isEmpty()) {
			log.info("📭 해당 포스트에 댓글이 없습니다. postId: {}", postId);
		}

		return comments; // 비어있더라도 그대로 리턴
	}

	@Transactional
	public void removeComment(Long commentId, Long requesterId) {
		Comments comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		Long postId = comment.getPosts().getId();
		Posts post = postService.findByIdAndDeletedIsFalse(postId, "Comment");

		if (!comment.getUsers().getId().equals(requesterId) && !post.getUser().getId().equals(requesterId)) {
			throw new UnauthorizedCommentAccessException();
		}

		log.info("🗑️ 댓글 삭제 요청 - Comment ID: {}, 요청자 ID: {}", commentId, requesterId);

		commentRepository.deleteById(commentId);

		log.info("✅ 댓글 삭제 완료 - Comment ID: {}", commentId);
	}
}
