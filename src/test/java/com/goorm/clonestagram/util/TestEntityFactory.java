package com.goorm.clonestagram.util;

import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import java.time.LocalDateTime;

public class TestEntityFactory {

    public static UserEntity createUser(String username) {
        return UserEntity.builder()
                .name(username)
                .build();
    }

    public static Posts createPost(UserEntity user, String content) {
        return Posts.builder()
                .user(user)
                .content(content)
                .mediaName("https://test.com/image.jpg") // ← 기본값 설정
                .build();
    }

    public static Feeds createFeed(UserEntity user, Posts post) {
        return Feeds.builder()
                .user(user)
                .post(post)
                .build();
    }
}

