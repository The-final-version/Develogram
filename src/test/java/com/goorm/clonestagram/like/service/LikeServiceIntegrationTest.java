package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;
import com.goorm.clonestagram.util.IntegrationTestHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.goorm.clonestagram.post.ContentType.IMAGE;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
@SpringBootTest
@Transactional
@Rollback
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
	IntegrationTestHelper testHelper;

	@Autowired
	@MockitoSpyBean
	private LikeRepository likeRepository;

	@BeforeEach
	public void setUp() {
		likeRepository.deleteAll();
		postsRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	public void testToggleLike() {
		// Given: 테스트용 사용자와 게시물 생성
		UserEntity user = UserEntity.builder()
			.name("testUser")
			.email("test@example.com")
			.password("password")
			.build();
		user = userRepository.save(user);

		Posts post = Posts.builder()
			.user(user)
			.content("Test Post")
			.contentType(IMAGE)
			.mediaName("test-url")
			.deleted(false)
			.build();
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
		UserEntity user1 = UserEntity.builder()
			.name("user1")
			.email("user1@example.com")
			.password("password1")
			.build();
		user1 = userRepository.save(user1);

		UserEntity user2 = UserEntity.builder()
			.name("user2")
			.email("user2@example.com")
			.password("password2")
			.build();
		user2 = userRepository.save(user2);

		Posts post = Posts.builder()
			.user(user1)
			.content("Test Post1")
			.contentType(IMAGE)
			.mediaName("test-url")
			.deleted(false)
			.build();
		post = postsRepository.save(post);

		// When: 두 사용자가 좋아요
		likeService.toggleLike(user1.getId(), post.getId());
		likeService.toggleLike(user2.getId(), post.getId());

		// Then: 좋아요 개수 확인
		assertEquals(2L, likeService.getLikeCount(post.getId()), "좋아요 개수가 2이어야 합니다.");
	}

	@Test
	public void testToggleLikeRapidly() throws InterruptedException {
		UserEntity user = testHelper.createUser("testUserForIntegrationTest");

		// UserEntity user = new UserEntity();
		// user.setUsername("testUser");
		// user.setEmail("test@example.com");
		// user.setPassword("password");
		// user.setDeleted(false);
		// user = userRepository.save(user);

		Posts post = Posts.builder()
			.user(user)
			.content("Test Post")
			.contentType(IMAGE)
			.mediaName("test-url")
			.deleted(false)
			.build();
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

		// // ✅ 트랜잭션 분리된 서비스에서 상태 조회
		// boolean liked = transactionalHelper.checkLiked(userId, postId);
		// assertThat(liked).isEqualTo(threadCount % 2 != 0);
		//
		// long likeCount = transactionalHelper.getLikeCount(postId);
		// assertThat(likeCount).isEqualTo(threadCount % 2 != 0 ? 1L : 0L);

	}
}
