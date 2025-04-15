package com.goorm.clonestagram.user.domain.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserExternalReadRepository;
import com.goorm.clonestagram.user.domain.repository.UserExternalWriteRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserExternalQueryService {
	private final UserExternalReadRepository userReadRepository;
	private final UserExternalWriteRepository userWriteRepository;

	public Page<User> searchUserByKeyword(String keyword, Pageable pageable) {
		return userReadRepository.searchUserByFullText(keyword, pageable);
	}
	public List<User> findByNameContainingIgnoreCase(String keyword) {
		return userReadRepository.findByNameContainingIgnoreCase(keyword);
	}
	public User findByIdAndDeletedIsFalse(Long userId) {
		return userReadRepository.findByIdAndDeletedIsFalse(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
	public boolean existsByIdAndDeletedIsFalse(Long userId) {
		return userReadRepository.existsByIdAndDeletedIsFalse(userId);
	}
}
