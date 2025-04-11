package com.goorm.clonestagram.user.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalReadRepository;
import com.goorm.clonestagram.user.domain.repository.UserInternalWriteRepository;
import com.goorm.clonestagram.user.domain.vo.UserEmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInternalQueryService {
	private final UserInternalReadRepository userReadRepository;
	private final UserInternalWriteRepository userWriteRepository;


	public Long findUserIdByname(String name) {
		return userReadRepository.findByName(name)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	public User findByEmail(String email) {
		return Optional.ofNullable(userReadRepository.findByEmail(email))
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	public void saveUser(User user) {
		userWriteRepository.save(user);
	}

	public void deleteUserId(Long userId) {
		userWriteRepository.deleteById(userId);
	}

	public boolean existsByEmail(String email) {
		return userReadRepository.existsByEmail(new UserEmail(email));
	}

	public User findUserById(Long userId) {
		return userReadRepository.findByIdAndDeletedIsFalse(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	public User findByIdAndDeletedIsFalse(Long userId) {
		return userReadRepository.findByIdAndDeletedIsFalse(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
