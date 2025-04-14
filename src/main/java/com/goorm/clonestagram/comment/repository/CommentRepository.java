package com.goorm.clonestagram.comment.repository;

import com.goorm.clonestagram.comment.domain.Comments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comments, Long> {
	List<Comments> findByPosts_Id(Long postId);

	void deleteAllByPosts_IdIn(List<Long> postIds);

	void deleteAllByUsers_Id(Long userId);
}
