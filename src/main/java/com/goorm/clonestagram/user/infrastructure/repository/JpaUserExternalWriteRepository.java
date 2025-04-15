package com.goorm.clonestagram.user.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@Repository
public interface JpaUserExternalWriteRepository extends JpaRepository<UserEntity, Long> {
	Object findByIdAndDeletedIsFalse(long eq);
}
