package com.goorm.clonestagram.follow.repository;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.user.domain.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follows, Long> {


    // 팔로우 관계 조회 메서드 추가
    Optional<Follows> findByFollowerAndFollowed(Users follower, Users followed);

    // ✅ 내가 팔로우한 사람들
    @Query("SELECT f FROM Follows f WHERE f.follower = :follower AND f.follower.deleted = false AND f.followed.deleted = false")
    List<Follows> findFollowingsByFollower(@Param("follower") Users follower);

    // ✅ 나를 팔로우한 사람들
    @Query("SELECT f FROM Follows f WHERE f.followed = :followed AND f.follower.deleted = false AND f.followed.deleted = false")
    List<Follows> findFollowersByFollowed(@Param("followed") Users followed);

    // ✅ 나를 팔로우하는 사람들의 ID
    @Query("SELECT f.follower.id FROM Follows f WHERE f.followed.id = :userId")
    List<Long> findFollowerIdsByFollowedId(@Param("userId") Long userId);

    // ✅ 내가 팔로우하는 사람들의 ID
    @Query("SELECT f.followed.id FROM Follows f WHERE f.follower.id = :userId")
    List<Long> findFollowingUserIdsByFollowerId(@Param("userId") Long userId);

    // 특정 사용자의 팔로워 수 가져오기
    @Query("SELECT COUNT(f) FROM Follows f WHERE f.followed.id = :userId AND f.followed.deleted = false")
    int getFollowerCount(@Param("userId") Long userId);

    // 특정 사용자의 팔로잉 수 가져오기
    @Query("SELECT COUNT(f) FROM Follows f WHERE f.follower.id = :userId AND f.follower.deleted = false")
    int getFollowingCount(@Param("userId") Long userId);
    // 내가 팔로우하는 사람 중 username에 keyword 포함된 유저
    @Query("SELECT f.followed FROM Follows f WHERE f.follower.id = :followerId AND f.followed.username LIKE %:keyword% AND f.followed.deleted = false")
    Page<Users> findFollowingByKeyword(@Param("followerId") Long followerId, @Param("keyword") String keyword, Pageable pageable);

    // 나를 팔로우하는 사람 중 username에 keyword 포함된 유저
    @Query("SELECT f.follower FROM Follows f WHERE f.followed.id = :followedId AND f.follower.username LIKE %:keyword% AND f.follower.deleted = false")
    Page<Users> findFollowerByKeyword(@Param("followedId") Long followedId, @Param("keyword") String keyword, Pageable pageable);

}
