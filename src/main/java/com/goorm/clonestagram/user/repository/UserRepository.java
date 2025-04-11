package com.goorm.clonestagram.user.repository;

import com.goorm.clonestagram.user.domain.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 사용자 정보를 처리하는 리포지토리 인터페이스
 * - 사용자와 관련된 데이터베이스 작업을 수행
 * - JpaRepository를 상속하여 기본적인 CRUD 연산을 제공
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByUsername(String username);
//    Users findByUsername(String username); // 필요 시

    @Query(value = """
            SELECT * FROM users
            WHERE MATCH(username) AGAINST(:keyword IN BOOLEAN MODE)
              AND deleted = false
        """, countQuery = """
            SELECT COUNT(*) FROM users
            WHERE MATCH(username) AGAINST(:keyword IN BOOLEAN MODE)
              AND deleted = false
        """, nativeQuery = true)
    Page<Users> searchUserByFullText(@Param("keyword") String keyword, Pageable pageable);

    //Todo 팔로우 엔티티 추가시 활성화
    @Query("SELECT f.followed.id FROM Follows f WHERE f.follower.id = :userId")
    List<Long> findFollowingUserIdsByFollowerId(@Param("userId") Long userId);

    Optional<Users> findByIdAndDeletedIsFalse(Long id);

    boolean existsByIdAndDeletedIsFalse(Long id);


    boolean existsByEmail(String email);

//    Users findByUsername(String email);

    Users findByEmail(String email);

    List<Users> findByUsernameContainingIgnoreCase(String keyword);
}
