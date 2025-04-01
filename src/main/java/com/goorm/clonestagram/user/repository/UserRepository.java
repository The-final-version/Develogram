package com.goorm.clonestagram.user.repository;

import com.goorm.clonestagram.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 정보를 처리하는 리포지토리 인터페이스
 * - 사용자와 관련된 데이터베이스 작업을 수행
 * - JpaRepository를 상속하여 기본적인 CRUD 연산을 제공
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query(value = """
    SELECT * FROM users
    WHERE MATCH(username) AGAINST(:keyword IN BOOLEAN MODE)
      AND deleted = false
""", countQuery = """
    SELECT COUNT(*) FROM users
    WHERE MATCH(username) AGAINST(:keyword IN BOOLEAN MODE)
      AND deleted = false
""", nativeQuery = true)
    Page<User> searchUserByFullText(@Param("keyword") String keyword, Pageable pageable);

    //Todo 팔로우 엔티티 추가시 활성화
    @Query("SELECT f.toUser.id FROM Follows f WHERE f.fromUser.id = :userId")
    List<Long> findFollowingUserIdsByFromUserId(@Param("userId") Long userId);

    Optional<User> findByIdAndDeletedIsFalse(Long id);
    boolean existsByIdAndDeletedIsFalse(Long id);


    boolean existsByEmail(String email);


    User findByEmail(String email);

    List<User> findByUsernameContainingIgnoreCase(String keyword);
}
