package com.goorm.clonestagram.follow.repository;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.user.domain.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FollowRepositoryTest {

    @Autowired
    FollowRepository followRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("T01_팔로우_저장_성공")
    void T01_팔로우_저장_성공() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");

        Follows follow = followRepository.save(new Follows(follower, followed));

        assertThat(follow.getId()).isNotNull();
        assertThat(follow.getFollower()).isEqualTo(follower);
        assertThat(follow.getFollowed()).isEqualTo(followed);
    }

    @Test
    @DisplayName("T02_팔로우_중복_조회")
    void T02_팔로우_중복_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        Optional<Follows> result = followRepository.findByFollowerAndFollowedWithLock(follower.getId(), followed.getId());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("T03_팔로잉_리스트_조회")
    void T03_팔로잉_리스트_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        List<Follows> result = followRepository.findFollowedAllByFollower(follower);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("T04_팔로워_리스트_조회")
    void T04_팔로워_리스트_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        List<Follows> result = followRepository.findFollowerAllByFollowed(followed);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("T05_팔로우중인_유저ID_목록_조회")
    void T05_팔로우중인_유저ID_목록_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        List<Long> ids = followRepository.findFollowedIdsByFollowerId(follower.getId());
        assertThat(ids).contains(followed.getId());
    }

    @Test
    @DisplayName("T06_팔로워ID_목록_조회")
    void T06_팔로워ID_목록_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        List<Long> ids = followRepository.findFollowerIdsByFollowedId(followed.getId());
        assertThat(ids).contains(follower.getId());
    }

    @Test
    @DisplayName("T07_팔로워_수_조회")
    void T07_팔로워_수_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        int count = followRepository.getFollowerCountByFollowedId(followed.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("T08_팔로잉_수_조회")
    void T08_팔로잉_수_조회() {
        Users follower = saveUser("userA");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        int count = followRepository.getFollowingCountByFollowerId(follower.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("T09_팔로잉_유저_검색")
    void T09_팔로잉_유저_검색() {
        Users follower = saveUser("userA");
        Users followed = saveUser("bobcat");
        followRepository.save(new Follows(follower, followed));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Users> page = followRepository.findFollowingByKeyword(follower.getId(), "bob", pageable);

        assertThat(page.getContent()).extracting("username").contains("bobcat");
    }

    @Test
    @DisplayName("T10_팔로워_유저_검색")
    void T10_팔로워_유저_검색() {
        Users follower = saveUser("alicecat");
        Users followed = saveUser("userB");
        followRepository.save(new Follows(follower, followed));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Users> page = followRepository.findFollowerByKeyword(followed.getId(), "alice", pageable);

        assertThat(page.getContent()).extracting("username").contains("alicecat");
    }

    // --- 유틸 메서드 ---
    private Users saveUser(String username) {
        Users user = Users.builder()
                .username(username)
                .password("pw")
                .email(username + "@test.com")
                .build();
        em.persist(user);
        return user;
    }
}
