package com.goorm.clonestagram.util;

import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

public class MockEntityFactory {

    public static UserEntity mockUser(Long id, String name) {
        // 1) 먼저 User 도메인 객체를 secureBuilder()로 생성 (isHashed=false, 즉 평문암호)
        User domainUser = User.secureBuilder()
            .id(id)
            .name(name)
            .email(name + "@example.com")
            .password("test1234!") // 여기서 입력된 비밀번호는 UserPassword에 의해 암호화됨
            .isHashed(false)
            .build();

        // 2) 도메인 User -> UserEntity 변환
        return UserEntity.from(domainUser);
    }

    public static Posts mockPost(Long id, String content, UserEntity owner) {
        return Posts.builder()
                .id(id)
                .content(content)
                .contentType(ContentType.IMAGE)
                .user(owner)
                .build();
    }

    public static Feeds mockFeed(UserEntity targetUser, Posts post) {
        return Feeds.builder()
                .user(targetUser)
                .post(post)
                .build();
    }
}
