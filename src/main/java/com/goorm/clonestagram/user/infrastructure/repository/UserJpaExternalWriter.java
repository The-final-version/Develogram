package com.goorm.clonestagram.user.infrastructure.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserExternalWriteRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserJpaExternalWriter implements UserExternalWriteRepository {

	private final JpaUserExternalWriteRepository jpaRepository;

	/* ===== 도메인 User 저장 ===== */
	@Override
	public User save(User user) {
		try {
			// 도메인 객체를 엔티티로 변환하는 정적 팩토리 메서드가 존재한다고 가정합니다.
			UserEntity userEntity = UserEntity.from(user);
			UserEntity savedEntity = jpaRepository.save(userEntity);
			return savedEntity.toDomain();
		} catch (DataAccessException e) {
			log.error("[DB] save(User) 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}

	/* ===== ID로 사용자 삭제 ===== */
	@Override
	public void deleteById(Long id) {
		try {
			jpaRepository.deleteById(id);
		} catch (DataAccessException e) {
			log.error("[DB] deleteById 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}
}
