package com.goorm.clonestagram.user.domain.repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

/**
 * 내부 도메인 로직에서 사용할 사용자 쓰기 전용 인터페이스
 */
public interface UserExternalWriteRepository {
	User save(User user);

	void deleteById(Long id);
}
