package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private PostsRepository postsRepository;

	@Autowired
	private PostService postService;

	@Autowired
	@MockitoSpyBean
	private LikeRepository likeRepository;

	@Test
	public void testToggleLike() {
		// Given: 테스트용 사용자와 게시물 생성
		Users user = new Users();
		user.setUsername("testUser");
		user.setEmail("test@example.com");
		user.setPassword("password");
		user.setDeleted(false);
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
		Users user1 = new Users();
		user1.setUsername("user1");
		user1.setEmail("user1@example.com");
		user1.setPassword("password1");
		user1.setDeleted(false);
		user1 = userRepository.save(user1);

		Users user2 = new Users();
		user2.setUsername("user2");
		user2.setEmail("user2@example.com");
		user2.setPassword("password2");
		user2.setDeleted(false);
		user2 = userRepository.save(user2);

		Posts post = new Posts();
		post.setUser(user1);
		post.setContent("Test Post1");
		post.setContentType(IMAGE);  // contents_type 추가
		post.setMediaName("test-url");   // media_url 추가 (nullable=false일 경우)
		post.setDeleted(false);
		post = postsRepository.save(post);

		// When: 두 사용자가 좋아요
		likeService.toggleLike(user1.getId(), post.getId());
		likeService.toggleLike(user2.getId(), post.getId());

		// Then: 좋아요 개수 확인
		assertEquals(2L, likeService.getLikeCount(post.getId()), "좋아요 개수가 2이어야 합니다.");
	}

	@Test
	public void testToggleLikeRapidly() throws InterruptedException {
		Users user = new Users();
		user.setUsername("testUser");
		user.setEmail("test@example.com");
		user.setPassword("password");
		user.setDeleted(false);
		user = userRepository.save(user);

		Posts post = new Posts();
		post.setUser(user);
		post.setContent("Test Post");
		post.setContentType(IMAGE);  // contents_type 추가
		post.setMediaName("test-url");   // media_url 추가 (nullable=false일 경우)
		post.setDeleted(false);
		post = postsRepository.save(post);

		Long userId = user.getId();
		Long postId = post.getId();

		// int threadCount = 11;
		// ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		// CountDownLatch latch = new CountDownLatch(threadCount);

		// for (int i = 0; i < threadCount; i++) {
		// 	executor.submit(() -> {
		// 		try {
		// 			likeService.toggleLike(userId, postId);
		// 		} finally {
		// 			latch.countDown();
		// 		}
		// 	});
		// }

		// latch.await(); // 모든 쓰레드 종료 대기
		// Thread.sleep(1000);

		int threadCount = 11; // 반복 횟수 (짝수일때 홀수일때에 따라 결과가 달라야 함)

		for (int i = 0; i < threadCount; i++) {
			likeService.toggleLike(userId, postId);
		}

		verify(likeRepository, times((threadCount + 1) / 2)).save(any(Like.class));
		verify(likeRepository, times(threadCount / 2)).delete(any(Like.class));

		boolean liked = likeService.isPostLikedByLoginUser(userId, postId);
		assertThat(liked).isEqualTo(threadCount % 2 != 0);
		assertThat(likeService.getLikeCount(postId)).isEqualTo(threadCount % 2 != 0 ? 1L : 0L);

	}
}
