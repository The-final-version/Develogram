package com.goorm.clonestagram.user.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@Repository
public interface JpaUserExternalReadRepository extends JpaRepository<UserEntity, Long> {

	@Query(value = """
            SELECT * FROM user
            WHERE MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)
              AND deleted = false
            """, countQuery = """
            SELECT COUNT(*) FROM user
            WHERE MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)
              AND deleted = false
            """, nativeQuery = true)
	Page<UserEntity> searchUserByFullText(@Param("keyword") String keyword, Pageable pageable);

	@Query("SELECT f.followed.id FROM Follows f WHERE f.follower.id = :userId")
	List<Long> findFollowingUserIdsByFollowerId(@Param("userId") Long userId);

	List<UserEntity> findByNameContainingIgnoreCase(String keyword);

	Optional<UserEntity> findByIdAndDeletedIsFalse(Long id);

	boolean existsByIdAndDeletedIsFalse(Long id);

	Optional<UserEntity> findByEmail(String email);
}
