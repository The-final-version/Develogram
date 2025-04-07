package com.goorm.clonestagram.user.domain.repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

/**
 * 외부(다른 도메인)에서 사용자 정보를 변경하는 쓰기 전용 기능을 위한 인터페이스
 */
public interface UserExternalWriteRepository {
	User save(User user);

	UserEntity save(UserEntity user);

	void deleteById(Long id);

	void deleteAll();
}
