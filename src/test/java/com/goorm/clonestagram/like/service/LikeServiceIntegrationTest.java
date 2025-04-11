package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.goorm.clonestagram.post.ContentType.IMAGE;
import static com.goorm.clonestagram.post.ContentType.VIDEO;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
@SpringBootTest
@Transactional
public class LikeServiceIntegrationTest {

	@Autowired
	private LikeService likeService;

	@Autowired
	private JpaUserExternalWriteRepository userRepository;

	@Autowired
	private UserExternalQueryService userService;

	@Autowired
	private PostsRepository postsRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private LikeRepository likeRepository;

	@Test
	public void testToggleLike() {
		// Given: 테스트용 사용자와 게시물 생성
		UserEntity user = new UserEntity(User.testMockUser("testUser"));
		user = userRepository.save(user);

		Posts post = new Posts();
		post.setUser(user);
		post.setContent("Test Post");
		post.setContentType(IMAGE);  // contents_type 추가
		post.setMediaName("test-url");   // media_url 추가 (nullable=false일 경우)
		post.setDeleted(false);
		post = postsRepository.save(post);

		// When: 좋아요 토글 (첫 번째 좋아요)
		likeService.toggleLike(user.getId(), post.getId());

		// Then: 좋아요가 제대로 생성되었는지 확인
		Optional<Like> like = likeRepository.findByUser_IdAndPost_Id(user.getId(), post.getId());
		assertTrue(like.isPresent(), "좋아요가 생성되어야 합니다.");
		assertEquals(1L, likeService.getLikeCount(post.getId()), "좋아요 개수가 1이어야 합니다.");

		// When: 좋아요 다시 토글 (좋아요 취소)
		likeService.toggleLike(user.getId(), post.getId());

		// Then: 좋아요가 제대로 취소되었는지 확인
		like = likeRepository.findByUser_IdAndPost_Id(user.getId(), post.getId());
		assertFalse(like.isPresent(), "좋아요가 취소되어야 합니다.");
		assertEquals(0L, likeService.getLikeCount(post.getId()), "좋아요 개수가 0이어야 합니다.");
	}

	@Test
	public void testGetLikeCount() {
		// Given: 테스트용 사용자와 게시물, 다중 좋아요 생성
		UserEntity user1 = new UserEntity(User.testMockUser("user1"));
		UserEntity user2 = new UserEntity(User.testMockUser("user2"));

		user1 = userRepository.save(user1);
		user2 = userRepository.save(user2);

		Posts post = new Posts();
		post.setUser(user1);
		post.setContent("Test Post1");
		post.setContentType(IMAGE);
		post.setMediaName("test-url");
		post.setDeleted(false);
		post = postsRepository.save(post);

		// When: 두 사용자가 좋아요
		likeService.toggleLike(user1.getId(), post.getId());
		likeService.toggleLike(user2.getId(), post.getId());

		// Then: 좋아요 개수 확인
		assertEquals(2L, likeService.getLikeCount(post.getId()), "좋아요 개수가 2이어야 합니다.");
	}

	// @Test
	// public void testToggleLikeTwiceRapidly() throws InterruptedException {
	// Todo
	// Users user = new Users();
	// user.setname("testUser");
	// user.setEmail("test@example.com");
	// user.setPassword("password");
	// user.setDeleted(false);
	// user = userRepository.save(user);
	//
	// Posts post = new Posts();
	// post.setUser(user);
	// post.setContent("Test Post");
	// post.setContentType(IMAGE);  // contents_type 추가
	// post.setMediaName("test-url");   // media_url 추가 (nullable=false일 경우)
	// post.setDeleted(false);
	// post = postsRepository.save(post);
	//
	// Long userId = user.getId();
	// Long postId = post.getId();
	//
	// int threadCount = 1;
	// ExecutorService executor = Executors.newFixedThreadPool(threadCount);
	// CountDownLatch latch = new CountDownLatch(threadCount);
	//
	// for (int i = 0; i < threadCount; i++) {
	// 	executor.submit(() -> {
	// 		try {
	// 			likeService.toggleLike(userId, postId);
	// 		} finally {
	// 			latch.countDown();
	// 		}
	// 	});
	// }
	//
	// latch.await(); // 모든 쓰레드 종료 대기
	//
	// boolean liked = likeService.isPostLikedByLoginUser(userId, postId);
	// System.out.println("최종 좋아요 상태: " + liked);
	// }
}
