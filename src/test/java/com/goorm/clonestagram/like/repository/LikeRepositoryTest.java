package com.goorm.clonestagram.like.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

@DataJpaTest
public class LikeRepositoryTest {

	@Autowired
	LikeRepository likeRepository;
	@Autowired
	PostsRepository postsRepository;
	@Autowired
	JpaUserExternalWriteRepository userRepository;

	UserEntity postWriter;
	Posts post1, post2, post3;
	UserEntity liker1, liker2, liker3;

	@BeforeEach
	void setUp() {
		postWriter = UserEntity.builder().name("Post Writer").password("1234").email("post@writer").build();
		userRepository.save(postWriter);
		post1 = Posts.builder().user(postWriter).content("Test Post").mediaName("test.jpg").build();
		post2 = Posts.builder().user(postWriter).content("Test Post").mediaName("test.jpg").build();
		post3 = Posts.builder().user(postWriter).content("Test Post").mediaName("test.jpg").build();
		postsRepository.saveAll(List.of(post1, post2, post3));
		liker1 = UserEntity.builder().name("Liker 1").password("1234").email("liker1@domain").build();
		userRepository.save(liker1);
		liker2 = UserEntity.builder().name("Liker 2").password("1234").email("liker2@domain").build();
		userRepository.save(liker2);
		liker3 = UserEntity.builder().name("Liker 3").password("1234").email("liker3@domain").build();
		userRepository.save(liker3);
	}

	@Test
	@DisplayName("포스트 ID로 조회에 성공하면 해당 포스트의 좋아요 리스트를 반환")
	void findByPost_IdTest_should_return_likes_list() {
		// given : 한 포스트 ID에 좋아요를 세개 등록한다.
		Like like1 = new Like(liker1.toDomain(), post1);
		Like like2 = new Like(liker2.toDomain(), post1);
		Like like3 = new Like(liker3.toDomain(), post1);
		likeRepository.saveAll(List.of(like1, like2, like3));

		// when : 해당 포스트 ID로 검색
		List<Like> result = likeRepository.findByPost_Id(post1.getId());

		// then : 리스트 반환, 리스트 크기 3인지 확인, 리스트 안의 유저 ID 확인,
		// 좋아요 ID는 자동생성이므로 확인X
		assertThat(result).hasSize(3);
		assertThat(result.stream().map(Like::getId))
			.containsExactlyInAnyOrder(like1.getId(), like2.getId(), like3.getId());
	}

	@Test
	@DisplayName("포스트 ID에 좋아요가 없는 경우, 빈 리스트를 반환")
	void findByPost_IdTest_should_return_empty_list() {
		// given : 한 포스트 ID에 좋아요를 등록하지 않는다.

		// when : 해당 포스트 ID로 검색
		List<Like> result = likeRepository.findByPost_Id(post1.getId());

		// then : 빈 리스트 반환
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("유저 ID와 포스트 ID로 등록된 좋아요가 있는지 확인한다.")
	void existsByUser_IdAndPost_Id_should_return_true_when_like_exists_and_false_when_not() {
		// given: liker1이 post에 좋아요를 등록한 상태
		Like like = new Like(liker1.toDomain(), post1);
		likeRepository.save(like);

		// when: liker1과 liker2로 해당 post에 대한 좋아요 존재 여부 확인
		boolean result1 = likeRepository.existsByUser_IdAndPost_Id(liker1.getId(), post1.getId());
		boolean result2 = likeRepository.existsByUser_IdAndPost_Id(liker2.getId(), post1.getId());

		// then: liker1은 true, liker2는 false 반환
		assertThat(result1).isTrue();
		assertThat(result2).isFalse();
	}

	@Test
	@DisplayName("포스트 ID로 조회 시 해당 포스트의 좋아요 수를 반환한다.")
	void countByPost_IdTest_should_return_like_count() {
		// given : post1에 좋아요 3개 등록, post2에 좋아요 2개 등록, post3에 좋아요 1개 등록
		Like like1 = new Like(liker1.toDomain(), post1);
		Like like2 = new Like(liker2.toDomain(), post1);
		Like like3 = new Like(liker3.toDomain(), post1);
		Like like4 = new Like(liker1.toDomain(), post2);
		Like like5 = new Like(liker2.toDomain(), post2);
		Like like6 = new Like(liker3.toDomain(), post3);
		likeRepository.saveAll(List.of(like1, like2, like3, like4, like5, like6));

		// when : 각 포스트에 대한 좋아요 수 조회
		Long resultForPost1 = likeRepository.countByPost_Id(post1.getId());
		Long resultForPost2 = likeRepository.countByPost_Id(post2.getId());
		Long resultForPost3 = likeRepository.countByPost_Id(post3.getId());

		// then : 3, 2, 1 반환
		assertThat(resultForPost1).isEqualTo(3L);
		assertThat(resultForPost2).isEqualTo(2L);
		assertThat(resultForPost3).isEqualTo(1L);
	}

	// @Test
	// @DisplayName("유저 ID와 포스트 ID로 등록된 좋아요를 반환한다.")
	// void findByUser_IdAndPost_Id_should_return_entity_if_exists_or_optional_empty_if_not() {
	// 	// given : liker1이 post1에 좋아요 등록
	// 	Like like = new Like(liker1.toDomain(), post1);
	// 	likeRepository.save(like);
	//
	// 	// when : liker1이 post1에 누른 좋아요와 liker2가 post1에 누른 좋아요 조회
	// 	Optional<Like> result1 = likeRepository.findByUser_IdAndPost_Id(liker1.getId(), post1.getId());
	// 	Optional<Like> result2 = likeRepository.findByUser_IdAndPost_Id(liker2.getId(), post1.getId());
	//
	// 	// then : liker1의 좋아요 반환, liker2의 좋아요는 없음
	// 	assertThat(result1).isPresent();
	// 	assertThat(result1.get().getUser().getId()).isEqualTo(liker1.getId());
	// 	assertThat(result1.get().getPost().getId()).isEqualTo(post1.getId());
	// 	assertThat(result2).isEmpty();
	// }
}
