package com.goorm.clonestagram.user.infrastructure.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalReadRepository;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.exception.user.error.UserNotFoundException;
import com.goorm.clonestagram.exception.user.error.UserDatabaseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserJpaInternalReader implements UserInternalReadRepository {
	private final JpaUserInternalReadRepository jpaRepository;

	/* ===== 닉네임으로 ID 조회 ===== */
	@Override
	public Optional<Long> findByName(String name) {
		try {
			return jpaRepository.findByName(name)
				.orElseThrow(UserNotFoundException::new)
				.getId()
				.describeConstable();
		} catch (DataAccessException e) {
			log.error("[DB] findByName 오류", e);
			throw new UserDatabaseException();
		}
	}

	/* ===== 이메일로 사용자 조회 ===== */
	@Override
	public User findByEmail(String email) {
		try {
			return jpaRepository.findByEmail(email)
				.orElseThrow(UserNotFoundException::new)
				.toDomain();
		} catch (DataAccessException e) {
			log.error("[DB] findByEmail 오류", e);
			throw new UserDatabaseException();
		}
	}


	/* ===== 존재 여부 ===== */
	@Override
	public boolean existsByIdAndDeletedIsFalse(Long id) {
		return jpaRepository.existsByIdAndDeletedIsFalse(id);
	}
	@Override
	public boolean existsByEmail(UserEmail email) {
		return jpaRepository.existsByEmail(email.getEmail());
	}


	/* ===== ID로 사용자 조회 ===== */
	@Override
	public Optional<User> findByIdAndDeletedIsFalse(Long id) {
		try {
			return Optional.ofNullable(
				jpaRepository.findByIdAndDeletedIsFalse(id)
					.orElseThrow(UserNotFoundException::new)
					.toDomain());
		} catch (DataAccessException e) {
			log.error("[DB] findByIdAndDeletedIsFalse 오류", e);
			throw new UserDatabaseException();
		}
	}
}
