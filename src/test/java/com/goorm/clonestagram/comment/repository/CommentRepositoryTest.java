package com.goorm.clonestagram.comment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.goorm.clonestagram.comment.domain.CommentEntity;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

@DataJpaTest
public class CommentRepositoryTest {
	// Comment Service에서 사용되는 Repository 메서드 리스트
	// commentRepository.save(comment)
	// commentRepository.findById(id)
	// commentRepository.findByPostsId(postId)
	// commentRepository.deleteById(id)
	@Autowired
	CommentRepository commentRepository;
	@Autowired
	PostsRepository postsRepository;
	@Autowired
	UserRepository userRepository;

	CommentEntity comment;
	Users users;
	Posts posts;

	@BeforeEach
	void setUp() {
		users = userRepository.save(Users.builder()
			.username("testuser")
			.password("password123")
			.email("testuser@example")
			.build());

		posts = postsRepository.save(Posts.builder()
			.user(users)
			.content("Test Post")
			.mediaName("http://example.com/image.jpg")
			.build());
	}

	// commentRepository.save(comment)
	@Nested
	@DisplayName("댓글 저장 테스트")
	class saveTest {
		@Test
		@DisplayName("댓글 저장을 성공하면 저장된 댓글 객체를 반환한다.")
		void success() {
			// given
			comment = CommentEntity.builder()
				.users(users)
				.posts(posts)
				.content("Confirm this text-saveTest")
				.build();

			// when
			CommentEntity savedComment = commentRepository.save(comment);

			//then
			assertThat(savedComment.getId()).isNotNull();
			assertThat(savedComment.getContent()).isEqualTo("Confirm this text-saveTest");
		}
	}

	// commentRepository.findById(id)
	@Nested
	@DisplayName("댓글 findById 테스트")
	class findByIdTest {
		@Test
		@DisplayName("댓글findById를 성공하면 찾는 댓글 객체를 반환하는지 확인하는 테스트")
		void success() {
			// given
			comment = CommentEntity.builder()
				.users(users)
				.posts(posts)
				.content("Confirm this text-findByIdTest")
				.build();

			// when
			commentRepository.save(comment);
			Optional<CommentEntity> foundComment = commentRepository.findById(comment.getId());

			// then
			assertThat(foundComment).isPresent();
			assertThat(foundComment.get().getId()).isEqualTo(comment.getId());
			assertThat(foundComment.get().getContent()).isEqualTo("Confirm this text-findByIdTest");
		}
	}

	// commentRepository.findByPosts_Id(postId)
	@Nested
	@DisplayName("post Id로 댓글 목록 조회 테스트")
	class findByPosts_IdTest {
		@Test
		@DisplayName("post Id로 댓글 목록을 조회하면 리스트를 반환하는지 확인하는 테스트")
		void success() {
			// given
			CommentEntity comment1 = CommentEntity.builder()
				.users(users).posts(posts).content("Confirm this text1")
				.build();
			CommentEntity comment2 = CommentEntity.builder()
				.users(users).posts(posts).content("Confirm this text2")
				.build();
			CommentEntity comment3 = CommentEntity.builder()
				.users(users).posts(posts).content("Confirm this text3")
				.build();
			commentRepository.save(comment1);
			commentRepository.save(comment2);
			commentRepository.save(comment3);

			// when
			List<CommentEntity> result = commentRepository.findByPosts_Id(posts.getId());

			//then
			assertThat(result).hasSize(3);
			assertThat(result).contains(comment1, comment2, comment3);
		}
	}

	// commentRepository.deleteById(id)
	@Nested
	@DisplayName("댓글 삭제 테스트")
	class deleteByIdTest {
		@Test
		@DisplayName("댓글을 삭제하고 나면 더이상 조회되지 않아야 한다.")
		void success() {
			// given
			comment = CommentEntity.builder()
				.users(users)
				.posts(posts)
				.content("Confirm this text-deleteByIdTest")
				.build();
			commentRepository.save(comment);

			// when
			commentRepository.deleteById(comment.getId());
			Optional<CommentEntity> foundComment = commentRepository.findById(comment.getId());

			// then
			assertThat(foundComment).isEmpty();
		}
	}
}
