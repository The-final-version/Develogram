package com.goorm.clonestagram.user.infrastructure.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalWriteRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserJpaInternalWriter implements UserInternalWriteRepository {

	private final JpaUserInternalWriteRepository jpaUserInternalWriteRepository;

	// 엔티티에 락을 걸어줍니다.
	@Override
	public User save(final User user) {
		UserEntity userEntity = new UserEntity(user);
		UserEntity savedEntity = jpaUserInternalWriteRepository.save(userEntity);
		return savedEntity.toDomain();
	}

	// 엔티티를 직접 저장하는 메소드에 락을 걸 수 있습니다.
	@Override
	@Lock(LockModeType.OPTIMISTIC)
	public UserEntity saveEntity(final UserEntity user) {
		return jpaUserInternalWriteRepository.save(user);
	}

	@Override
	public void deleteById(Long id) {
		jpaUserInternalWriteRepository.deleteById(id);
	}
}
