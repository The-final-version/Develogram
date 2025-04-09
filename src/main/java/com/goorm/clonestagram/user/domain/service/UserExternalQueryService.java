package com.goorm.clonestagram.user.domain.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.goorm.clonestagram.exception.user.error.UserNotFoundException;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalReadRepository;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserExternalQueryService {
	private final JpaUserExternalReadRepository userExternalReadRepository;
	private final JpaUserExternalWriteRepository userExternalWriteRepository;

	private static final String USER_NOT_FOUND_MESSAGE = "해당 사용자가 존재하지 않습니다.";

	public Page<UserEntity> searchUserByKeyword(String keyword, Pageable pageable) {
		return userExternalReadRepository.searchUserByFullText(keyword, pageable);
	}

	public List<UserEntity> findByName_NameContainingIgnoreCase(String keyword) {
		return userExternalReadRepository.findByNameContainingIgnoreCase(keyword);
	}

	public UserEntity findByIdAndDeletedIsFalse(Long userId) {
		return userExternalReadRepository.findByIdAndDeletedIsFalse(userId)
			.orElseThrow(UserNotFoundException::new);
	}

	public boolean existsByIdAndDeletedIsFalse(Long userId) {
		return userExternalReadRepository.existsByIdAndDeletedIsFalse(userId);
	}
}
