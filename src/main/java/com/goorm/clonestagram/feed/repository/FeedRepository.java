package com.goorm.clonestagram.feed.repository;

import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.post.domain.Posts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feeds, Long> {

    /**
     * 유저의 피드를 게시물 + 작성자(user)까지 함께 페치 조인하여 조회 (페이징 지원)
     */
    @Query("SELECT f FROM Feeds f " +
            "JOIN FETCH f.post p " +
            "JOIN FETCH p.user u " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.createdAt DESC")
    Page<Feeds> findByUserIdWithPostAndUser(@Param("userId") Long userId, Pageable pageable);


    @Query("SELECT f FROM Posts f WHERE f.deletedAt IS NULL")
    Page<Posts> findAllByDeletedIsFalse(Pageable pageable);


    @Query("SELECT f FROM Posts f WHERE f.user.id IN :followIds AND f.deletedAt IS NULL")
    Page<Posts> findAllByUserIdInAndDeletedIsFalse(@Param("followIds") List<Long> followIds, Pageable pageable);


    void deleteByUserIdAndPostIdIn(Long userId, List<Long> postIds);
    /**
     * 특정 유저의 전체 피드 조회 (테스트 또는 삭제용)
     */
    List<Feeds> findByUserId(Long userId);

    void deleteByPostId(Long postId);
}
