package com.goorm.clonestagram.user.infrastructure.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalWriteRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserJpaInternalWriter implements UserInternalWriteRepository {

	private final JpaUserInternalWriteRepository jpaUserInternalWriteRepository;

	@Override
	public User save(final User user) {
		return jpaUserInternalWriteRepository.save(new UserEntity(user))
			.toDomain();
	}

	@Override
	public void deleteById(Long id) {
		((CrudRepository<UserEntity, Long>) jpaUserInternalWriteRepository).deleteById(id);
	}
}
