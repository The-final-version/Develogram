package com.goorm.clonestagram.hashtag.repository;

import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostHashTagRepository extends JpaRepository<PostHashTags, Long> {

	@Query("""
		    SELECT p FROM PostHashTags ph
		    JOIN ph.posts p
		    JOIN ph.hashTags h
		    WHERE h.tagContent LIKE %:keyword%
		    AND p.deleted = false
		    ORDER BY p.createdAt DESC
		""")
	Page<Posts> findPostsByHashtagKeyword(@Param("keyword") String keyword, Pageable pageable);

	void deleteAllByPostsId(Long id);

	long countByHashTags(HashTags hashTags);
}
