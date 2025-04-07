package com.goorm.clonestagram.user.domain.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

	private final String USER_NOT_FOUND_MESSAGE = "해당 사용자가 존재하지 않습니다.";

	public Long findUserIdByUsername(String username) {
		return userReadRepository.findByName(username)
			.orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
	}

	public User findByEmail(String email) {
		return Optional.ofNullable(userReadRepository.findByEmail(email))
			.orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE + " email = " + email));
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
			.orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
	}

	public User findByIdAndDeletedIsFalse(Long userId) {
		return userReadRepository.findByIdAndDeletedIsFalse(userId)
			.orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
	}
}
