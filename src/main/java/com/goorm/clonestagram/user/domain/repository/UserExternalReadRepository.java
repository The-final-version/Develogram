package com.goorm.clonestagram.user.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.goorm.clonestagram.user.domain.entity.User;

/**
 * 외부(다른 도메인)에서 사용자 프로필 조회 등 읽기 전용 기능을 위한 도메인 인터페이스
 */
public interface UserExternalReadRepository {
	Page<User> searchUserByFullText(String keyword, Pageable pageable);
	List<Long> findFollowingUserIdsByFollowerId(Long userId);
	List<User> findByNameContainingIgnoreCase(String keyword);
	Optional<User> findByIdAndDeletedIsFalse(Long id);
	boolean existsByIdAndDeletedIsFalse(Long id);
	Optional<User> findByEmail(String email);
}
