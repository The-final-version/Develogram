package com.goorm.clonestagram.user.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@Repository
public interface JpaUserInternalWriteRepository extends JpaRepository<UserEntity, Long> {

	/**
	 * 사용자 엔티티를 저장합니다.
	 *
	 * @param userEntity 저장할 사용자 엔티티
	 * @return 저장된 사용자 엔티티
	 */
	UserEntity save(UserEntity userEntity);

	User save(User userEntity);

	/**
	 * 사용자 엔티티를 삭제합니다.
	 *
	 * @param userEntity 삭제할 사용자 엔티티
	 */
	void delete(UserEntity userEntity);
}
