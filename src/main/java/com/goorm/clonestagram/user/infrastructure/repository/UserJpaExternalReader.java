package com.goorm.clonestagram.user.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserExternalReadRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserJpaExternalReader implements UserExternalReadRepository {

	private final JpaUserExternalReadRepository jpaRepository;

	/* ===== FULL TEXT 검색 ===== */
	@Override
	public Page<User> searchUserByFullText(String keyword, Pageable pageable) {
		try {
			return jpaRepository.searchUserByFullText(keyword, pageable)
				.map(UserEntity::toDomain);
		} catch (DataAccessException e) {
			log.error("[DB] searchUserByFullText 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}

	/* ===== 팔로잉 사용자 ID 조회 ===== */
	@Override
	public List<Long> findFollowingUserIdsByFollowerId(Long userId) {
		try {
			return jpaRepository.findFollowingUserIdsByFollowerId(userId);
		} catch (DataAccessException e) {
			log.error("[DB] findFollowingUserIdsByFollowerId 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}

	/* ===== 사용자 이름으로 검색 ===== */
	@Override
	public List<User> findByNameContainingIgnoreCase(String keyword) {
		try {
			return jpaRepository.findByNameContainingIgnoreCase(keyword)
				.stream()
				.map(UserEntity::toDomain)
				.collect(Collectors.toList());
		} catch (DataAccessException e) {
			log.error("[DB] findByNameContainingIgnoreCase 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}

	/* ===== ID로 사용자 조회 (삭제되지 않은 데이터) ===== */
	@Override
	public Optional<User> findByIdAndDeletedIsFalse(Long id) {
		try {
			return Optional.ofNullable(
				jpaRepository.findByIdAndDeletedIsFalse(id)
					.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
					.toDomain()
			);
		} catch (DataAccessException e) {
			log.error("[DB] findByIdAndDeletedIsFalse 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}

	/* ===== 삭제되지 않은 존재 여부 확인 ===== */
	@Override
	public boolean existsByIdAndDeletedIsFalse(Long id) {
		try {
			return jpaRepository.existsByIdAndDeletedIsFalse(id);
		} catch (DataAccessException e) {
			log.error("[DB] existsByIdAndDeletedIsFalse 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}

	/* ===== 이메일로 사용자 조회 ===== */
	@Override
	public Optional<User> findByEmail(String email) {
		try {
			return Optional.ofNullable(
				jpaRepository.findByEmail(email)
					.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
					.toDomain()
			);
		} catch (DataAccessException e) {
			log.error("[DB] findByEmail 오류", e);
			throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
		}
	}
}
