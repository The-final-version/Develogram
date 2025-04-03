package com.goorm.clonestagram.util;

import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.Users;

public class MockEntityFactory {

    public static Users mockUser(Long id, String username) {
        return Users.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .password("test1234")
                .build();
    }

    public static Posts mockPost(Long id, String content, Users owner) {
        return Posts.builder()
                .id(id)
                .content(content)
                .contentType(ContentType.IMAGE)
                .user(owner)
                .build();
    }

    public static Feeds mockFeed(Users targetUser, Posts post) {
        return Feeds.builder()
                .user(targetUser)
                .post(post)
                .build();
    }
}
