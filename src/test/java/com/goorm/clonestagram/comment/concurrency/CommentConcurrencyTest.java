package com.goorm.clonestagram.comment.concurrency;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.comment.service.CommentService;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;

@SpringBootTest
@Rollback(false) // 실제 insert 결과를 검증해야 하므로 롤백 방지
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommentConcurrencyTest {

	private static final int THREAD_COUNT = 100; // 테스트 횟수

	@Autowired
	CommentService commentService;
	@Autowired
	CommentRepository commentRepository;
	@Autowired
	LikeService likeService;
	@Autowired
	LikeRepository likeRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	PostsRepository postsRepository;

	private Long postId;
	private List<Long> userIds;
	private Long likeUserId;

	@BeforeAll
	void setUp() {
		Users postWriter = Users.builder().username("postWriter").password("1234").email("post@writer").build();
		userRepository.save(postWriter);
		Posts post = Posts.builder().user(postWriter).content("test post").mediaName("test.jpg").deleted(false).build();
		postsRepository.save(post);
		postId = post.getId();

		userIds = new ArrayList<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			Users user = Users.builder()
				.username("user" + i)
				.email("user" + i + "@test.com")
				.password("1234")
				.deleted(false)
				.build();
			userRepository.save(user);
			userIds.add(user.getId());
		}

		Users likeTester = Users.builder().username("likeTester").password("1234").email("likeTester@domain").build();
		userRepository.save(likeTester);
		likeUserId = likeTester.getId();
	}

	@Test
	@DisplayName("동시에 여러 댓글 생성")
	void concurrentComments() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(20);
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			final int index = i;
			executor.submit(() -> {
				try {
					CommentRequest req = new CommentRequest(userIds.get(index), postId, "댓글 " + index);
					commentService.createComment(req);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		List<Comments> comments = commentRepository.findByPosts_Id(postId);
		assertThat(comments).hasSize(THREAD_COUNT);
	}

	@Test
	@DisplayName("한 사람이 동일한 좋아요를 여러번 토글")
	void concurrentLikeToggle() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					likeService.toggleLike(likeUserId, postId);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		boolean liked = likeService.isPostLikedByLoginUser(postId, likeUserId);
		Assertions.assertEquals(THREAD_COUNT % 2 != 0, liked);

		long likeCount = likeRepository.countByPost_Id(postId);
		Assertions.assertEquals(THREAD_COUNT % 2 != 0 ? 1L : 0L, likeCount);
	}

	@Test
	@DisplayName("동시에 여러 사용자가 좋아요")
	void concurrentLikeFromMultipleUsers() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(20);
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			final int index = i;
			executor.submit(() -> {
				try {
					likeService.toggleLike(userIds.get(index), postId);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// long likeCount = likeRepository.countByPost_Id(postId);
		long likeCount = likeService.getLikeCount(postId);
		Assertions.assertEquals(THREAD_COUNT, likeCount);
	}

}
