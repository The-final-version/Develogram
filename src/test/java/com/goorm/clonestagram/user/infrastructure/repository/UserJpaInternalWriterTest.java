package com.goorm.clonestagram.user.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserJpaInternalWriter.class)
class UserJpaInternalWriterTest {

	@Autowired
	private JpaUserInternalWriteRepository jpaUserInternalWriteRepository;

	@Autowired
	private UserJpaInternalWriter userJpaInternalWriter;

	@Test
	@DisplayName("save(User): 도메인 객체를 저장 후 변환된 도메인 객체 반환")
	void testSaveDomainUser() {
		// given: 도메인 객체 생성 (User.builder()가 존재한다고 가정)
		User domainUser = User.builder()
			.email(new UserEmail("test@example.com"))
			.name(new UserName("Test User"))
			.password(new UserPassword("password"))
			.build();

		// when: 도메인 객체 저장
		User savedUser = userJpaInternalWriter.save(domainUser);

		// then: 반환된 도메인 객체가 null이 아니고, 필드 값이 일치하며, DB에도 저장되어야 함
		assertThat(savedUser).isNotNull();
		assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
		assertThat(savedUser.getName()).isEqualTo("Test User");
		assertThat(savedUser.getId()).isNotNull();

		// 추가: underlying DB에 저장된 UserEntity와 도메인 값 비교
		Optional<UserEntity> entityOpt = jpaUserInternalWriteRepository.findById(savedUser.getId());
		assertThat(entityOpt).isPresent();
		UserEntity entity = entityOpt.get();
		assertThat(entity.getEmail()).isEqualTo("test@example.com");
		assertThat(entity.getName()).isEqualTo("Test User");
	}

	@Test
	@DisplayName("deleteById: id로 도메인 엔티티 삭제")
	void testDeleteById() {
		// given: 도메인 객체 저장
		User domainUser = User.builder()
			.email(new UserEmail("delete@example.com"))
			.name(new UserName("Delete User"))
			.password(new UserPassword("password"))
			.build();
		User savedUser = userJpaInternalWriter.save(domainUser);
		Long id = savedUser.getId();
		assertThat(jpaUserInternalWriteRepository.findById(id)).isPresent();

		// when: ID로 삭제
		userJpaInternalWriter.deleteById(id);

		// then: 삭제 후 DB에서 해당 엔티티가 존재하지 않아야 함
		Optional<UserEntity> deletedOpt = jpaUserInternalWriteRepository.findById(id);
		assertThat(deletedOpt).isNotPresent();
	}
}
