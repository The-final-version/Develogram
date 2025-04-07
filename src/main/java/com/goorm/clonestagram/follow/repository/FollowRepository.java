package com.goorm.clonestagram.follow.repository;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

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

	Optional<Follows> findByFollowerAndFollowed(UserEntity follower, UserEntity followed);

	// 팔로잉 목록 (내가 팔로우한 유저들)
	@Query("SELECT f FROM Follows f WHERE f.follower = :follower")
	List<Follows> findFollowedAllByFollower(@Param("follower") UserEntity follower);

	// 팔로워 목록 (나를 팔로우한 유저들)
	@Query("SELECT f FROM Follows f WHERE f.followed = :followed")
	List<Follows> findFollowerAllByFollowed(@Param("followed") UserEntity followed);

	// 나를 팔로우한 유저 ID 목록
	@Query("SELECT f.follower.id FROM Follows f WHERE f.followed.id = :userId")
	List<Long> findFollowerIdsByFollowedId(@Param("userId") Long userId);

	// 내가 팔로우한 유저 ID 목록
	@Query("SELECT f.followed.id FROM Follows f WHERE f.follower.id = :userId")
	List<Long> findFollowedIdsByFollowerId(@Param("userId") Long userId);

	@Query("SELECT COUNT(f) FROM Follows f WHERE f.followed.id = :userId")
	int getFollowerCountByFollowedId(@Param("userId") Long userId);

	@Query("SELECT COUNT(f) FROM Follows f WHERE f.follower.id = :userId")
	int getFollowingCountByFollowerId(@Param("userId") Long userId);

	// 팔로잉 검색
	@Query("SELECT f.followed FROM Follows f WHERE f.follower.id = :followerId AND f.followed.name LIKE %:keyword%")
	Page<UserEntity> findFollowingByKeyword(@Param("followerId") Long followerId, @Param("keyword") String keyword,
		Pageable pageable);

	// 팔로워 검색
	@Query("SELECT f.follower FROM Follows f WHERE f.followed.id = :followedId AND f.follower.name LIKE %:keyword%")
	Page<UserEntity> findFollowerByKeyword(@Param("followedId") Long followedId, @Param("keyword") String keyword,
		Pageable pageable);
}
