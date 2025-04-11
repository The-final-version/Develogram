package com.goorm.clonestagram.follow.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follows {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id")
    private UserEntity follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id")
    private UserEntity followed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Follows(User follower, User followed) {
        this.follower = new UserEntity(follower);   // 유저 도메인 수정
        this.followed = new UserEntity(followed);   // 유저 도메인 수정
        this.createdAt = LocalDateTime.now(); // 현재 시간 자동 저장
    }



}
