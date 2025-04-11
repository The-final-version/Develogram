package com.goorm.clonestagram.user.domain.repository;

import java.util.Optional;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.UserEmail;

/**
 * 내부 도메인 로직에서 사용할 사용자 읽기 전용 인터페이스
 */
public interface UserInternalReadRepository {
	Optional<Long> findByName(String name);

	User findByEmail(String email);

	boolean existsByIdAndDeletedIsFalse(Long id);
	boolean existsByEmail(UserEmail email);
	Optional<User> findByIdAndDeletedIsFalse(Long id);
}
