package com.goorm.clonestagram.user.infrastructure.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalReadRepository;
import com.goorm.clonestagram.user.domain.vo.UserEmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserJpaInternalReader implements UserInternalReadRepository {
	private final String USER_NOT_FOUND_MESSAGE = "해당 사용자가 존재하지 않습니다.";
	private final JpaUserInternalReadRepository jpaRepository;

	@Override
	public Optional<Long> findByName(String name) {
		return jpaRepository.findByName(name)
			.orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE + " name = " + name)).getId()
			.describeConstable();
	}

	@Override
	public User findByEmail(String email) {
		return jpaRepository.findByEmail(email)
			.orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE + " email = " + email))
			.toDomain();
	}

	@Override
	public boolean existsByIdAndDeletedIsFalse(Long id) {
		return jpaRepository.existsByIdAndDeletedIsFalse(id);
	}

	@Override
	public boolean existsByEmail(UserEmail email) {
		return jpaRepository.existsByEmail(email.getEmail());
	}

	@Override
	public Optional<User> findByIdAndDeletedIsFalse(Long id) {
		return Optional.ofNullable(jpaRepository.findByIdAndDeletedIsFalse(id)
			.orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE + ", id=" + id)).toDomain());
	}
}
