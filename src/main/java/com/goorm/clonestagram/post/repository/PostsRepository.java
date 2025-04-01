package com.goorm.clonestagram.post.repository;

import com.goorm.clonestagram.post.domain.Posts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Posts와 관련된 JPA
 */
public interface PostsRepository extends JpaRepository<Posts, Long> {

    Optional<Posts> findByIdAndDeletedIsFalse(Long id);
    Page<Posts> findAllByUserIdAndDeletedIsFalse(Long userId, Pageable pageable);

    List<Posts> findAllByUserIdAndDeletedIsFalse(Long userId);

    boolean existsByIdAndDeletedIsFalse(Long id);

    Page<Posts> findAllByDeletedIsFalse(Pageable pageable);


}
