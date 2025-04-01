package com.goorm.clonestagram.like.repository;

import com.goorm.clonestagram.like.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    List<Like> findByPost_Id(Long postId); // 특정 게시물에 대한 좋아요 조회
    Boolean existsByUserIdAndPost_Id(Long userId, Long postsId);
    Long countByPost_Id(Long postId); // 좋아요 개수 확인
    Optional<Like> findByUser_IdAndPost_Id(Long userId, Long postsId);


}
