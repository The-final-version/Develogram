package com.goorm.clonestagram.like.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.clonestagram.like.domain.Like;

import jakarta.persistence.LockModeType;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
	List<Like> findByPost_Id(Long postId); // 특정 게시물에 대한 좋아요 조회

	Boolean existsByUser_IdAndPost_Id(Long userId, Long postsId);

	Long countByPost_Id(Long postId); // 좋아요 개수 확인

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.post.id = :postsId")
	Optional<Like> findByUser_IdAndPost_Id(Long userId, Long postsId);

	@Query("SELECT lc.likeCount FROM LikeCount lc WHERE lc.postId = :postId")
	Optional<Long> findLikeCount(@Param("postId") Long postId);

	@Modifying
	@Transactional
	@Query("UPDATE LikeCount lc SET lc.likeCount = :count WHERE lc.postId = :postId")
	void updateLikeCount(@Param("postId") Long postId, @Param("count") Long count);

	@Query("SELECT COUNT(lc) > 0 FROM LikeCount lc WHERE lc.postId = :postId")
	boolean existsLikeCountByPostId(@Param("postId") Long postId);

	@Modifying
	@Transactional
	@Query("INSERT INTO LikeCount(postId, likeCount) VALUES (:postId, :likeCount)")
	void saveLikeCount(@Param("postId") Long postId, @Param("likeCount") Long likeCount);

}
