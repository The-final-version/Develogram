package com.goorm.clonestagram.user.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@Repository
public interface JpaUserInternalReadRepository extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByName(String name);
	Optional<UserEntity> findByEmail(String email);
	boolean existsByIdAndDeletedIsFalse(Long id);
	boolean existsByEmail(String email);
	Optional<UserEntity> findByIdAndDeletedIsFalse(Long id);
}
