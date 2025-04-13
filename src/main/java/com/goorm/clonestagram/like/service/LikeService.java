package com.goorm.clonestagram.like.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {
	private final LikeRepository likeRepository;
	private final UserService userService;
	private final PostService postService;

	// 좋아요 토글
	@Transactional
	public void toggleLike(Long userId, Long postId) {
		Users user = userService.findByIdAndDeletedIsFalse(userId);
		Posts post = postService.findByIdAndDeletedIsFalse(postId);

		// userId와 postId를 사용해 좋아요 여부 확인
		// 락 걸고 조회
		Optional<Like> existingLike = likeRepository.findByUser_IdAndPost_Id(userId, postId);

		if (existingLike.isPresent()) {
			likeRepository.delete(existingLike.get()); // 좋아요 취소
		} else {
			try {
				likeRepository.save(new Like(user, post)); // 좋아요 추가
			} catch (DataIntegrityViolationException e) {
				// 동시에 두 요청이 온 경우 하나는 성공하고 하나는 이곳으로 옴.
				if (likeRepository.existsByUser_IdAndPost_Id(userId, postId)) {
					log.warn("중복 좋아요 요청 감지: userId={}, postId={}", userId, postId);
				}
			}
		}

		syncLikeCount(postId);
	}

	@Transactional
	public void syncLikeCount(Long postId) {
		Long count = likeRepository.countByPost_Id(postId);

		if (likeRepository.existsLikeCountByPostId(postId)) {
			likeRepository.updateLikeCount(postId, count);
		} else {
			likeRepository.saveLikeCount(postId, count);
		}
	}

	@Transactional(readOnly = true)
	public Long getLikeCount(Long postId) {
		if (!postService.existsByIdAndDeletedIsFalse(postId)) {
			throw new PostNotFoundException(postId);
		}

		// return likeRepository.countByPost_Id(postId);
		return likeRepository.findLikeCount(postId).orElse(0L);
	}

	public boolean isPostLikedByLoginUser(Long postId, Long userId) {
		Users user = userService.findByIdAndDeletedIsFalse(userId);
		Posts post = postService.findByIdAndDeletedIsFalse(postId);

		return likeRepository.existsByUser_IdAndPost_Id(userId, postId);
	}

}
