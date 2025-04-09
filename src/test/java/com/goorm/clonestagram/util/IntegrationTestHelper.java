package com.goorm.clonestagram.util;

import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class IntegrationTestHelper {

    private final UserRepository userRepository;
    private final PostsRepository postRepository;
    private final CommentRepository commentRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public IntegrationTestHelper(UserRepository userRepository,
                                 PostsRepository postRepository,
                                 CommentRepository commentRepository,
                                 BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * 임의의 유저를 생성합니다.
     */
    public Users createUser(String baseUsername) {
        // baseUsername이 너무 길면 잘라주기
        String trimmedBase = baseUsername.length() > 12 ? baseUsername.substring(0, 12) : baseUsername;

        // 짧은 UUID (5자만 사용)
        String suffix = UUID.randomUUID().toString().substring(0, 5);

        String uniqueUsername = trimmedBase + "_" + suffix;

        Users user = Users.builder()
                .username(uniqueUsername)
                .password(bCryptPasswordEncoder.encode("password")) // ✅ 여기 중요
                .email(uniqueUsername + "@example.com")
                .build();
        return userRepository.save(user);
    }

    /**
     * 유저와 연관된 모든 post, comment 를 삭제한 후 유저를 삭제합니다.
     */
    @Transactional
    public void deleteUserAndDependencies(Users user) {
        // comment 삭제 (연관관계: Comments.users)
        commentRepository.deleteAllByUsers_Id(user.getId());

        // post 삭제 (연관관계: Posts.users)
        postRepository.deleteAllByUser_Id(user.getId());

        // 유저 삭제
        userRepository.deleteById(user.getId());
    }
}
