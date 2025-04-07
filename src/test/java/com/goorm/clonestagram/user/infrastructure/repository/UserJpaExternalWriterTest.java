package com.goorm.clonestagram.user.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserJpaExternalWriter.class)
class UserJpaExternalWriterTest {

	@Autowired
	private JpaUserExternalWriteRepository jpaWriteRepository;

	@Autowired
	private UserJpaExternalWriter userJpaExternalWriter;

	@Test
	@DisplayName("save(User): 도메인 객체를 저장하고, 변환된 도메인 객체 반환")
	void testSaveDomainUser() {
		// given: 도메인 객체 생성
		User domainUser = User.builder()
			.email(new UserEmail("testdomain@example.com"))
			.name(new UserName("TestDomain"))
			.password(new UserPassword("password"))
			.build();

		// when: 도메인 객체 저장
		User savedUser = userJpaExternalWriter.save(domainUser);

		// then: 반환된 도메인 객체가 정상적으로 저장되어 동일한 필드 값을 갖는지 확인
		assertThat(savedUser).isNotNull();
		assertThat(savedUser.getEmail()).isEqualTo("testdomain@example.com");
		assertThat(savedUser.getName()).isEqualTo("TestDomain");
	}

	@Test
	@DisplayName("save(UserEntity): 엔티티를 저장하고, ID가 부여된 UserEntity 반환")
	void testSaveUserEntity() {
		// given: UserEntity 생성 (빌더 사용)
		UserEntity userEntity = UserEntity.builder()
			.email("testentity@example.com")
			.name("TestEntity")
			.build();

		// when: 엔티티 저장
		UserEntity savedEntity = userJpaExternalWriter.save(userEntity);

		// then: 저장 후 ID가 부여되고, 필드 값이 일치하는지 확인
		assertThat(savedEntity).isNotNull();
		assertThat(savedEntity.getId()).isNotNull();
		assertThat(savedEntity.getEmail()).isEqualTo("testentity@example.com");
		assertThat(savedEntity.getName()).isEqualTo("TestEntity");
	}

	@Test
	@DisplayName("deleteById: ID로 사용자 삭제")
	void testDeleteById() {
		// given: 먼저 UserEntity를 저장하고 flush()하여 영속성 컨텍스트 초기화
		UserEntity userEntity = UserEntity.builder()
			.email("delete@example.com")
			.name("ToDelete")
			.build();
		userEntity = jpaWriteRepository.saveAndFlush(userEntity);
		Long id = userEntity.getId();
		assertThat(jpaWriteRepository.findById(id)).isPresent();

		// when: ID로 삭제
		userJpaExternalWriter.deleteById(id);

		// then: 삭제 후 해당 ID로 조회 시 결과가 없어야 함
		Optional<UserEntity> deletedOpt = jpaWriteRepository.findById(id);
		assertThat(deletedOpt).isNotPresent();
	}

	@Test
	@DisplayName("deleteAll: 모든 사용자 삭제")
	void testDeleteAll() {
		// given: 두 건의 UserEntity 저장
		UserEntity user1 = UserEntity.builder()
			.email("a@example.com")
			.name("UserA")
			.build();
		UserEntity user2 = UserEntity.builder()
			.email("b@example.com")
			.name("UserB")
			.build();
		jpaWriteRepository.saveAndFlush(user1);
		jpaWriteRepository.saveAndFlush(user2);
		List<UserEntity> before = jpaWriteRepository.findAll();
		assertThat(before).hasSizeGreaterThanOrEqualTo(2);

		// when: 전체 삭제
		userJpaExternalWriter.deleteAll();

		// then: 전체 조회 시 빈 리스트여야 함
		List<UserEntity> after = jpaWriteRepository.findAll();
		assertThat(after).isEmpty();
	}
}
