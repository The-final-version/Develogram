package com.goorm.clonestagram.follow.repository;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
class FollowRepositoryTest {

    @Autowired
    FollowRepository followRepository;

    @Autowired
    EntityManager em;

    @MockitoBean
    UserEntity follower;

    @MockitoBean
    UserEntity followed;

    @Test
    @DisplayName("T01_팔로우_저장_성공")
    void T01_팔로우_저장_성공() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");

        Follows follow = followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        assertThat(follow.getId()).isNotNull();
        assertThat(follow.getFollower()).isEqualTo(follower);
        assertThat(follow.getFollowed()).isEqualTo(followed);
    }

    @Test
    @DisplayName("T02_팔로우_중복_조회")
    void T02_팔로우_중복_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));
        log.debug("follower: {}, followed: {}", follower, followed);
        Optional<Follows> result = followRepository.findByFollowerAndFollowedWithLock(follower,
			followed);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("T03_팔로잉_리스트_조회")
    void T03_팔로잉_리스트_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        List<Follows> result = followRepository.findFollowedAllByFollower(follower);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("T04_팔로워_리스트_조회")
    void T04_팔로워_리스트_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        List<Follows> result = followRepository.findFollowerAllByFollowed(followed);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("T05_팔로우중인_유저ID_목록_조회")
    void T05_팔로우중인_유저ID_목록_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        List<Long> ids = followRepository.findFollowedIdsByFollowerId(follower.getId());
        assertThat(ids).contains(followed.getId());
    }

    @Test
    @DisplayName("T06_팔로워ID_목록_조회")
    void T06_팔로워ID_목록_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        List<Long> ids = followRepository.findFollowerIdsByFollowedId(followed.getId());
        assertThat(ids).contains(follower.getId());
    }

    @Test
    @DisplayName("T07_팔로워_수_조회")
    void T07_팔로워_수_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        int count = followRepository.getFollowerCountByFollowedId(followed.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("T08_팔로잉_수_조회")
    void T08_팔로잉_수_조회() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        int count = followRepository.getFollowingCountByFollowerId(follower.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("T09_팔로잉_유저_검색")
    void T09_팔로잉_유저_검색() {
        UserEntity follower = saveUser("userA");
        UserEntity followed = saveUser("bobcat");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = followRepository.findFollowingByKeyword(follower.getId(), "bob", pageable);

        assertThat(page.getContent()).extracting("name").contains("bobcat");
    }

    @Test
    @DisplayName("T10_팔로워_유저_검색")
    void T10_팔로워_유저_검색() {
        UserEntity follower = saveUser("alicecat");
        UserEntity followed = saveUser("userB");
        followRepository.save(new Follows(follower.toDomain(), followed.toDomain()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = followRepository.findFollowerByKeyword(followed.getId(), "alice", pageable);

        assertThat(page.getContent()).extracting("name").contains("alicecat");
    }

    // --- 유틸 메서드 ---
    private UserEntity saveUser(String name) {
        UserEntity user = UserEntity.builder()
                .name(name)
                .password("pw")
                .email(name + "@test.com")
                .build();
        em.persist(user);
        return user;
    }
}
