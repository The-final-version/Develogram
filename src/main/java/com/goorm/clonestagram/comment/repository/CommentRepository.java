package com.goorm.clonestagram.comment.repository;

import com.goorm.clonestagram.comment.domain.CommentEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
	List<CommentEntity> findByPosts_Id(Long postId);
}
