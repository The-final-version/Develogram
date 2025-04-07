package com.goorm.clonestagram.comment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

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
	JpaUserExternalWriteRepository userRepository;

	Comments comment;
	UserEntity users;
	Posts posts;

	@BeforeEach
	void setUp() {
		users = userRepository.save(UserEntity.builder()
			.username("testuser")
			.password("password")
			.email("email@test.com")
			.profileImgUrl("http://example.com/image.jpg")
			.profileBio("Test Bio")
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
	class SaveTest {
		@Test
		@DisplayName("댓글 저장을 성공하면 저장된 댓글 객체를 반환한다.")
		void success() {
			// given
			comment = Comments.builder()
				.users(users)
				.posts(posts)
				.content("Confirm this text-saveTest")
				.build();

			// when
			Comments savedComment = commentRepository.save(comment);

			//then
			assertThat(savedComment.getId()).isNotNull();
			assertThat(savedComment.getContent()).isEqualTo("Confirm this text-saveTest");
		}
	}

	// commentRepository.findById(id)
	@Nested
	@DisplayName("댓글 findById 테스트")
	class FindByIdTest {
		@Test
		@DisplayName("댓글findById를 성공하면 찾는 댓글 객체를 반환하는지 확인하는 테스트")
		void success() {
			// given
			comment = Comments.builder()
				.users(users)
				.posts(posts)
				.content("Confirm this text-findByIdTest")
				.build();

			// when
			commentRepository.save(comment);
			Optional<Comments> foundComment = commentRepository.findById(comment.getId());

			// then
			assertThat(foundComment).isPresent();
			assertThat(foundComment.get().getId()).isEqualTo(comment.getId());
			assertThat(foundComment.get().getContent()).isEqualTo("Confirm this text-findByIdTest");
		}
	}

	// commentRepository.findByPosts_Id(postId)
	@Nested
	@DisplayName("post Id로 댓글 목록 조회 테스트")
	class FindByPosts_IdTest {
		@Test
		@DisplayName("post Id로 댓글 목록을 조회하면 리스트를 반환하는지 확인하는 테스트")
		void success() {
			// given
			Comments comment1 = Comments.builder()
				.users(users).posts(posts).content("Confirm this text1")
				.build();
			Comments comment2 = Comments.builder()
				.users(users).posts(posts).content("Confirm this text2")
				.build();
			Comments comment3 = Comments.builder()
				.users(users).posts(posts).content("Confirm this text3")
				.build();
			commentRepository.save(comment1);
			commentRepository.save(comment2);
			commentRepository.save(comment3);

			// when
			List<Comments> result = commentRepository.findByPosts_Id(posts.getId());

			//then
			assertThat(result).hasSize(3);
			assertThat(result).contains(comment1, comment2, comment3);
		}
	}

	// commentRepository.deleteById(id)
	@Nested
	@DisplayName("댓글 삭제 테스트")
	class DeleteByIdTest {
		@Test
		@DisplayName("댓글을 삭제하고 나면 더이상 조회되지 않아야 한다.")
		void success() {
			// given
			comment = Comments.builder()
				.users(users)
				.posts(posts)
				.content("Confirm this text-deleteByIdTest")
				.build();
			commentRepository.save(comment);

			// when
			commentRepository.deleteById(comment.getId());
			Optional<Comments> foundComment = commentRepository.findById(comment.getId());

			// then
			assertThat(foundComment).isEmpty();
		}
	}
}
