package com.goorm.clonestagram.comment.controller;

import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.dto.CommentResponse;
import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.service.CommentService;
import com.goorm.clonestagram.common.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.util.StringUtils;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/comments")
public class CommentController {
	private final CommentService commentService;
	private final IdempotencyService idempotencyService;

	@GetMapping("/{id}")
	public CommentResponse getCommentById(@PathVariable("id") Long id) {
		Comments entity = commentService.getCommentById(id);

		// if (entity == null) {
		// 	return CommentResponse.builder().build();
		// }
		return CommentResponse.builder()
			.id(entity.getId())
			.userId(entity.getUsers().getId())
			.postId(entity.getPosts().getId())
			.content(entity.getContent())
			.createdAt(entity.getCreatedAt())
			.build();

	}

	@GetMapping("/post/{postId}")
	public List<CommentResponse> getCommentsByPostId(@PathVariable("postId") Long postId) {
		List<Comments> entities = Optional.ofNullable(commentService.getCommentsByPostId(postId))
			.orElse(Collections.emptyList());

		return entities.stream()
			.map(entity -> CommentResponse.builder()
				.id(entity.getId())
				.userId(entity.getUsers().getId())
				.postId(entity.getPosts().getId())
				.content(entity.getContent())
				.createdAt(entity.getCreatedAt())
				.build())
			.collect(Collectors.toList());
	}

	@PostMapping
	public ResponseEntity<CommentResponse> create(
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestBody CommentRequest request
	) throws Exception {
		// 멱등성 키를 사용하여 서비스 호출
		Comments entity;
		if (StringUtils.hasText(idempotencyKey)) { // 멱등성 키가 있는 경우
			entity = idempotencyService.executeWithIdempotency(idempotencyKey,
					() -> commentService.createComment(request),
					Comments.class);
		} else { // 멱등성 키가 없는 경우
			entity = commentService.createComment(request); // 직접 서비스 호출
		}

		CommentResponse response = CommentResponse.builder()
			.id(entity.getId())
			.userId(entity.getUsers().getId())
			.name(entity.getUsers().getName()) 	// 유저 도메인 수정
			.postId(entity.getPosts().getId())
			.content(entity.getContent())
			.createdAt(entity.getCreatedAt())
			.build();

		URI location = URI.create("/comments/" + response.getId());

		return ResponseEntity
			.created(location)
			.body(response);
	}

	// 아래 방식처럼 requester의 id값을 클라이언트가 넘기는 것은 보안의 위험이 있음.
	// 리팩터링 해야함. 지금은 메모만 해둠
	// Todo
	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable("commentId") Long commentId, @RequestParam("requesterId") Long requesterId) {
		commentService.removeComment(commentId, requesterId);

		return ResponseEntity.noContent().build();
	}
}
