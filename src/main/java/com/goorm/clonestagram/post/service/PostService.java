package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.feed.repository.FeedRepository;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import com.goorm.clonestagram.exception.PostNotFoundException;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

	private final UserService userService;

	private final PostsRepository postsRepository;

	public boolean existsByIdAndDeletedIsFalse(Long postId) {
		return postsRepository.existsByIdAndDeletedIsFalse(postId);
	}

	public Posts findByIdAndDeletedIsFalse(Long postId) {
		return postsRepository.findByIdAndDeletedIsFalse(postId)
			.orElseThrow(() -> new PostNotFoundException("게시물이 없습니다. postId: " + postId));
	}

	public Posts findByIdAndDeletedIsFalse(Long postId, String from) {
		return postsRepository.findByIdAndDeletedIsFalse(postId)
			.orElseThrow(
				() -> new PostNotFoundException("댓글이 속한 게시글이 존재하지 않습니다. postId: " + postId + ", from: " + from));
	}

	public PostResDto getMyPosts(Long userId, Pageable pageable) {
		//1. userId를 활용해 유저 객체 조회
		Users users = userService.findByIdAndDeletedIsFalse(userId);

		//2. 해당 유저가 작성한 모든 피드 조회, 페이징 처리
		Page<Posts> myFeed = postsRepository.findAllByUserIdAndDeletedIsFalse(users.getId(), pageable);

		//3. 모든 작업이 완료도니 경우 응답 반환
		return PostResDto.builder()
			.user(UserProfileDto.fromEntity(users))
			.feed(myFeed.map(PostInfoDto::fromEntity))
			.build();
	}

	public List<Posts> findAllByUserIdAndDeletedIsFalse(Long userId) {
		return postsRepository.findAllByUserIdAndDeletedIsFalse(userId);
	}

	public Posts save(Posts postEntity) {
		return postsRepository.save(postEntity);
	}

	public Posts saveAndFlush(Posts postEntity) {
		return postsRepository.saveAndFlush(postEntity);
	}

	public Posts findByIdWithPessimisticLock(Long id) {
		return postsRepository.findByIdWithPessimisticLock(id)
			.orElseThrow(() -> new PostNotFoundException("게시물이 없습니다. postId: " + id));
	}
}
