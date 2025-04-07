package com.goorm.clonestagram.user.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserExternalReadRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserJpaExternalReader implements UserExternalReadRepository {

	private final JpaUserExternalReadRepository jpaRepository;

	@Override
	public Page<User> searchUserByFullText(String keyword, Pageable pageable) {
		return jpaRepository.searchUserByFullText(keyword, pageable)
			.map(UserEntity::toDomain);
	}

	@Override
	public List<Long> findFollowingUserIdsByFollowerId(Long userId) {
		return jpaRepository.findFollowingUserIdsByFollowerId(userId);
	}

	@Override
	public List<User> findByNameContainingIgnoreCase(String keyword) {
		return jpaRepository.findByNameContainingIgnoreCase(keyword)
			.stream()
			.map(UserEntity::toDomain)
			.collect(Collectors.toList());
	}

	@Override
	public Optional<User> findByIdAndDeletedIsFalse(Long id) {
		return jpaRepository.findByIdAndDeletedIsFalse(id)
			.map(UserEntity::toDomain);
	}

	@Override
	public boolean existsByIdAndDeletedIsFalse(Long id) {
		return jpaRepository.existsByIdAndDeletedIsFalse(id);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return jpaRepository.findByEmail(email)
			.map(UserEntity::toDomain);
	}
}
