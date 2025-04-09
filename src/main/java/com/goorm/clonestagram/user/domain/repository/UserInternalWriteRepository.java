package com.goorm.clonestagram.user.domain.repository;

import org.springframework.data.jpa.repository.Lock;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import jakarta.persistence.LockModeType;

/**
 * 내부 도메인 로직에서 사용할 사용자 쓰기 전용 인터페이스
 */
public interface UserInternalWriteRepository {

	@Lock(LockModeType.OPTIMISTIC)
	User save(User user);

	UserEntity saveEntity(UserEntity user);



	void deleteById(Long id);
}
