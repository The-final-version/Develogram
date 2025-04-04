package com.goorm.clonestagram.feed.repository;

import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.util.TestEntityFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FeedRepositoryTest {

    @Autowired
    FeedRepository feedRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("유저 ID로 피드를 페이징 조회한다")
    void findByUserIdWithPostAndUser() {
        // given
        Users user = TestEntityFactory.createUser("testuser");
        Posts post = TestEntityFactory.createPost(user, "Hello");

        em.persist(user);
        em.persist(post);

        Feeds feed = TestEntityFactory.createFeed(user, post);
        em.persist(feed);
        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        var page = feedRepository.findByUserIdWithPostAndUser(user.getId(), pageable);

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getPost().getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("삭제되지 않은 모든 피드를 조회한다")
    void findAllByDeletedIsFalse() {
        // given
        Users user = TestEntityFactory.createUser("testuser");
        Posts post = TestEntityFactory.createPost(user, "Hello");
        Feeds feed = TestEntityFactory.createFeed(user, post);

        em.persist(user);
        em.persist(post);
        em.persist(feed);
        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        var result = feedRepository.findAllByDeletedIsFalse(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("팔로우 유저들의 피드를 조회한다")
    void findAllByUserIdInAndDeletedIsFalse() {
        // given
        Users user1 = TestEntityFactory.createUser("user1");
        Users user2 = TestEntityFactory.createUser("user2");

        Posts post1 = TestEntityFactory.createPost(user1, "hello");
        Posts post2 = TestEntityFactory.createPost(user2, "world");

        Feeds feed1 = TestEntityFactory.createFeed(user1, post1);
        Feeds feed2 = TestEntityFactory.createFeed(user2, post2);

        em.persist(user1);
        em.persist(user2);
        em.persist(post1);
        em.persist(post2);
        em.persist(feed1);
        em.persist(feed2);
        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);
        List<Long> userIds = List.of(user1.getId(), user2.getId());

        // when
        var result = feedRepository.findAllByUserIdInAndDeletedIsFalse(userIds, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("유저 ID와 postIds로 피드를 삭제한다")
    void deleteByUserIdAndPostIdIn() {
        // given
        Users user = TestEntityFactory.createUser("user1");
        Posts p1 = TestEntityFactory.createPost(user, "A");
        Posts p2 = TestEntityFactory.createPost(user, "B");

        Feeds f1 = TestEntityFactory.createFeed(user, p1);
        Feeds f2 = TestEntityFactory.createFeed(user, p2);

        em.persist(user);
        em.persist(p1);
        em.persist(p2);
        em.persist(f1);
        em.persist(f2);
        em.flush();
        em.clear();

        // when
        feedRepository.deleteByUserIdAndPostIdIn(user.getId(), List.of(p1.getId(), p2.getId()));
        em.flush();
        em.clear();

        // then
        List<Feeds> result = feedRepository.findByUserId(user.getId());
        assertThat(result).isEmpty();
    }
}
