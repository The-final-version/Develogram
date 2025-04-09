package com.goorm.clonestagram.like.controller;

import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.util.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LikeController {

	private final LikeService likeService;

	// 특정 게시글의 좋아요 개수 조회
	@GetMapping("/feeds/{postId}/likes")
	public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
		Long likeCount = likeService.getLikeCount(postId);
		return ResponseEntity.ok(likeCount);
	}

	// 특정 게시글에 좋아요 추가 또는 취소 (토글)
	@PostMapping("/feeds/{postId}/likes")
	public ResponseEntity<Void> toggleLike(@PathVariable Long postId,
		@AuthenticationPrincipal CustomUserDetails userDetail) {
		Long userId = userDetail.getId();
		likeService.toggleLike(userId, postId);  // 서비스 레이어에서 토글 처리
		return ResponseEntity.ok().build();
	}

	@GetMapping("/posts/{postId}/liked")
	public ResponseEntity<Boolean> checkIfLiked(@PathVariable Long postId,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		boolean liked = likeService.isPostLikedByLoginUser(postId, userDetails.getId());
		return ResponseEntity.ok(liked);
	}
}
