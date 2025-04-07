package com.goorm.clonestagram.user.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserJpaExternalReader.class)
class UserJpaExternalReaderTest {

	@Autowired
	private JpaUserExternalReadRepository jpaRepository;

	@Autowired
	private UserJpaExternalReader userJpaExternalReader;

	@Test
	@DisplayName("findByNameContainingIgnoreCase: 키워드가 포함된 사용자 검색")
	void testFindByNameContainingIgnoreCase() {
		// given: 테스트용 데이터 등록
		UserEntity user1 = UserEntity.builder()
			.email("test1@example.com")
			.name("Alice")
			.build();
		UserEntity user2 = UserEntity.builder()
			.email("test2@example.com")
			.name("alice cooper")
			.build();
		UserEntity user3 = UserEntity.builder()
			.email("test3@example.com")
			.name("Bob")
			.build();
		jpaRepository.saveAndFlush(user1);
		jpaRepository.saveAndFlush(user2);
		jpaRepository.saveAndFlush(user3);

		// when: "alice"라는 키워드로 검색
		List<User> result = userJpaExternalReader.findByNameContainingIgnoreCase("alice");

		// then: 이름에 "alice"가 포함된 두 건의 결과가 조회되어야 함
		assertThat(result).hasSize(2);
		assertThat(result)
			.extracting(User::getEmail)
			.containsExactlyInAnyOrder("test1@example.com", "test2@example.com");
	}

	@Test
	@DisplayName("findByIdAndDeletedIsFalse: 삭제되지 않은 사용자 검색")
	void testFindByIdAndDeletedIsFalse() {
		// given: 활성 사용자와 삭제된 사용자 등록
		UserEntity activeUser = UserEntity.builder()
			.email("active@example.com")
			.name("ActiveUser")
			.build();
		UserEntity deletedUser = UserEntity.builder()
			.email("deleted@example.com")
			.name("DeletedUser")
			.build();
		// 삭제 상태 변경: delete() 호출 시 deleted가 true로, deletedAt가 기록됨
		deletedUser.delete();
		activeUser = jpaRepository.saveAndFlush(activeUser);
		deletedUser = jpaRepository.saveAndFlush(deletedUser);

		// when
		Optional<User> activeResult = userJpaExternalReader.findByIdAndDeletedIsFalse(activeUser.getId());
		Optional<User> deletedResult = userJpaExternalReader.findByIdAndDeletedIsFalse(deletedUser.getId());

		// then: 활성 사용자는 조회되고, 삭제된 사용자는 조회되지 않아야 함
		assertThat(activeResult).isPresent();
		assertThat(deletedResult).isNotPresent();
	}

	@Test
	@DisplayName("existsByIdAndDeletedIsFalse: 존재하는 활성 사용자 여부 확인")
	void testExistsByIdAndDeletedIsFalse() {
		// given: 활성 사용자 등록
		UserEntity user = UserEntity.builder()
			.email("exist@example.com")
			.name("ExistUser")
			.build();
		user = jpaRepository.saveAndFlush(user);

		// when
		boolean exists = userJpaExternalReader.existsByIdAndDeletedIsFalse(user.getId());
		boolean notExists = userJpaExternalReader.existsByIdAndDeletedIsFalse(999L); // 존재하지 않는 ID

		// then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
	}

	@Test
	@DisplayName("findByEmail: 이메일로 사용자 검색")
	void testFindByEmail() {
		// given: 사용자 등록
		UserEntity user = UserEntity.builder()
			.email("findme@example.com")
			.name("FindMe")
			.build();
		user = jpaRepository.saveAndFlush(user);

		// when
		Optional<User> foundUser = userJpaExternalReader.findByEmail("findme@example.com");
		Optional<User> notFoundUser = userJpaExternalReader.findByEmail("unknown@example.com");

		// then
		assertThat(foundUser).isPresent();
		assertThat(foundUser.get().getEmail()).isEqualTo("findme@example.com");
		assertThat(notFoundUser).isNotPresent();
	}
}
