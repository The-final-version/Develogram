package com.goorm.clonestagram.user.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalReadRepository;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserJpaInternalReader.class)
public class UserJpaInternalReaderTest {

	@Autowired
	private JpaUserInternalReadRepository jpaRepository;

	@Autowired
	private UserInternalReadRepository userInternalReadRepository;

	@Test
	@DisplayName("findByName: 사용자 이름으로 ID 조회 성공")
	void testFindByNameSuccess() {
		// given: 사용자 생성 및 저장
		UserEntity userEntity = UserEntity.builder()
			.email("alice@example.com")
			.name("Alice")
			.build();
		userEntity = jpaRepository.saveAndFlush(userEntity);

		// when: 이름으로 조회
		Optional<Long> optionalId = userInternalReadRepository.findByName("Alice");

		// then: 조회된 id와 저장된 id가 일치해야 함
		assertThat(optionalId).isPresent();
		Long id = optionalId.get();
		assertThat(id).isEqualTo(userEntity.getId());
	}

	@Test
	@DisplayName("findByName: 사용자 이름으로 조회 실패 - 사용자 없음")
	void testFindByNameNotFound() {
		// when & then: 존재하지 않는 이름 조회 시 RuntimeException 발생
		RuntimeException exception = assertThrows(RuntimeException.class,
			() -> userInternalReadRepository.findByName("NonExistingName"));
		assertThat(exception.getMessage()).contains("해당 사용자가 존재하지 않습니다.");
	}

	@Test
	@DisplayName("findByEmail: 이메일로 사용자 조회 성공")
	void testFindByEmailSuccess() {
		// given: 사용자 생성 및 저장
		UserEntity userEntity = UserEntity.builder()
			.email("bob@example.com")
			.name("Bob")
			.build();
		userEntity = jpaRepository.saveAndFlush(userEntity);

		// when: 이메일로 조회
		User user = userInternalReadRepository.findByEmail("bob@example.com");

		// then: 반환된 도메인 객체의 email과 name이 일치해야 함
		assertThat(user).isNotNull();
		assertThat(user.getEmail()).isEqualTo("bob@example.com");
		assertThat(user.getName()).isEqualTo("Bob");
	}

	@Test
	@DisplayName("findByEmail: 이메일로 조회 실패 - 사용자 없음")
	void testFindByEmailNotFound() {
		// when & then: 존재하지 않는 이메일 조회 시 RuntimeException 발생
		RuntimeException exception = assertThrows(RuntimeException.class,
			() -> userInternalReadRepository.findByEmail("nonexistent@example.com"));
		assertThat(exception.getMessage()).contains("해당 사용자가 존재하지 않습니다.");
	}

	@Test
	@DisplayName("existsByIdAndDeletedIsFalse: 활성 사용자 존재 여부 확인")
	void testExistsByIdAndDeletedIsFalse() {
		// given: 활성 사용자와 삭제된 사용자 등록
		UserEntity activeUser = UserEntity.builder()
			.email("active@example.com")
			.name("ActiveUser")
			.build();
		UserEntity deletedUser = UserEntity.builder()
			.email("deleted@example.com")
			.name("DeletedUser")
			.build();
		activeUser = jpaRepository.saveAndFlush(activeUser);
		deletedUser = jpaRepository.saveAndFlush(deletedUser);
		// 삭제 처리
		deletedUser.delete();
		jpaRepository.saveAndFlush(deletedUser);

		// when:
		boolean existsActive = userInternalReadRepository.existsByIdAndDeletedIsFalse(activeUser.getId());
		boolean existsDeleted = userInternalReadRepository.existsByIdAndDeletedIsFalse(deletedUser.getId());

		// then:
		assertThat(existsActive).isTrue();
		assertThat(existsDeleted).isFalse();
	}

	@Test
	@DisplayName("existsByEmail: 활성 사용자 이메일 존재 여부 확인")
	void testExistsByEmail() {
		// given: 사용자 생성 및 저장
		UserEntity userEntity = UserEntity.builder()
			.email("charlie@example.com")
			.name("Charlie")
			.build();
		userEntity = jpaRepository.saveAndFlush(userEntity);

		// when:
		boolean exists = userInternalReadRepository.existsByEmail(new UserEmail("charlie@example.com"));
		boolean notExists = userInternalReadRepository.existsByEmail(new UserEmail("unknown@example.com"));

		// then:
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
	}

	@Test
	@DisplayName("findByIdAndDeletedIsFalse: 활성 사용자 조회 성공")
	void testFindByIdAndDeletedIsFalseSuccess() {
		// given: 활성 사용자 등록
		UserEntity userEntity = UserEntity.builder()
			.email("david@example.com")
			.name("David")
			.build();
		userEntity = jpaRepository.saveAndFlush(userEntity);

		// when: 활성 사용자 조회
		Optional<User> optionalUser = userInternalReadRepository.findByIdAndDeletedIsFalse(userEntity.getId());

		// then:
		assertThat(optionalUser).isPresent();
		User user = optionalUser.get();
		assertThat(user.getEmail()).isEqualTo("david@example.com");
		assertThat(user.getName()).isEqualTo("David");
	}

	@Test
	@DisplayName("findByIdAndDeletedIsFalse: 조회 실패 - 삭제된 사용자")
	void testFindByIdAndDeletedIsFalseDeleted() {
		// given: 삭제된 사용자 등록
		UserEntity userEntity = UserEntity.builder()
			.email("eve@example.com")
			.name("Eve")
			.build();
		userEntity = jpaRepository.saveAndFlush(userEntity);
		// 삭제 처리
		userEntity.delete();
		jpaRepository.saveAndFlush(userEntity);

		// when & then: 조회 시 RuntimeException 발생
		UserEntity finalUserEntity = userEntity;
		RuntimeException exception = assertThrows(RuntimeException.class,
			() -> userInternalReadRepository.findByIdAndDeletedIsFalse(finalUserEntity.getId()));
		assertThat(exception.getMessage()).contains("해당 사용자가 존재하지 않습니다.");
	}
}
