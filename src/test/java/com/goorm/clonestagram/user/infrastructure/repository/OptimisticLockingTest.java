package com.goorm.clonestagram.user.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@DataJpaTest
class OptimisticLockingTest {

	@Autowired
	private JpaUserInternalWriteRepository userWriteRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void whenConcurrentUpdates_thenOptimisticLockingException() {
		// 1. 신규 사용자 엔티티 생성 후 저장
		UserEntity user = new UserEntity("test@example.com", "password", "testUser");
		user = userWriteRepository.save(user);
		entityManager.flush();
		entityManager.clear();

		// 2. 동일 엔티티를 두 번 조회하여 두 개의 인스턴스를 준비합니다.
		UserEntity userInstance1 = entityManager.find(UserEntity.class, user.getId());
		UserEntity userInstance2 = entityManager.find(UserEntity.class, user.getId());

		// 3. 첫 번째 인스턴스에 대해 간단한 필드(예: name) 업데이트
		userInstance1.setName(userInstance1.getName() + "_updated1");
		userWriteRepository.save(userInstance1);
		entityManager.flush();

		// 4. 두 번째 인스턴스는 여전히 구버전 상태이므로 name 필드 변경 시도
		userInstance2.setName(userInstance2.getName() + "_updated2");

		// 5. 두 번째 인스턴스 저장 시도 시, 버전 충돌로 낙관적 락 예외 발생을 기대합니다.
		assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
			userWriteRepository.save(userInstance2);
			entityManager.flush();
		});
	}
}
