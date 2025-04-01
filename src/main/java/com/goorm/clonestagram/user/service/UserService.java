package com.goorm.clonestagram.user.service;

import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public boolean existsByIdAndDeletedIsFalse(Long postId) {
        return userRepository.existsByIdAndDeletedIsFalse(postId);
    }


    public Long findUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저가 존재하지 않습니다."))
                .getId();
    }

    public Users findByIdAndDeletedIsFalse(Long userId) {
        return userRepository.findByIdAndDeletedIsFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

    }
}