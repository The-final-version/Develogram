package com.goorm.clonestagram.user.domain.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserExternalReadRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserExternalQueryService {
	private final UserExternalReadRepository userExternalReadRepository;

	private static final String USER_NOT_FOUND_MESSAGE = "해당 사용자가 존재하지 않습니다.";

	public Page<User> searchUserByKeyword(String keyword, Pageable pageable) {
		return userExternalReadRepository.searchUserByFullText(keyword, pageable);
	}

	public List<User> findByName_NameContainingIgnoreCase(String keyword) {
		return userExternalReadRepository.findByNameContainingIgnoreCase(keyword);
	}

	public User findByIdAndDeletedIsFalse(Long userId) {
		return userExternalReadRepository.findByIdAndDeletedIsFalse(userId)
				.orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
	}

	public boolean existsByIdAndDeletedIsFalse(Long userId) {
		return userExternalReadRepository.existsByIdAndDeletedIsFalse(userId);
	}
}
