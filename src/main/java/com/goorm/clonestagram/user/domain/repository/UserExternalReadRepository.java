package com.goorm.clonestagram.user.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

/**
 * 내부 도메인 로직에서 사용할 사용자 읽기 전용 인터페이스
 */
public interface UserExternalReadRepository {
	Page<User> searchUserByFullText(@Param("keyword") String keyword, Pageable pageable);

	List<Long> findFollowingUserIdsByFollowerId(@Param("userId") Long userId);

	List<User> findByNameContainingIgnoreCase(String keyword);

	Optional<User> findByIdAndDeletedIsFalse(Long id);

	boolean existsByIdAndDeletedIsFalse(Long id);

	Optional<User> findByEmail(String email);
}
