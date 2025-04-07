package com.goorm.clonestagram.user.infrastructure.repository;

import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserExternalWriteRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserJpaExternalWriter implements UserExternalWriteRepository {

	private final JpaUserExternalWriteRepository jpaUserExternalWriteRepository;

	@Override
	public User save(final User user) {
		return jpaUserExternalWriteRepository.save(new UserEntity(user))
			.toDomain();
	}

	@Override
	public UserEntity save(UserEntity user) {
		return jpaUserExternalWriteRepository.save(user);
	}

	@Override
	public void deleteById(Long id) {
		jpaUserExternalWriteRepository.deleteById(id);
	}

	@Override
	public void deleteAll() {
		jpaUserExternalWriteRepository.deleteAll();
	}
}
