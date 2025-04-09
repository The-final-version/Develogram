package com.goorm.clonestagram.user.domain.service;

import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.UserNotFoundException;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalReadRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserExternalQueryService 단위 테스트
 * - 최대한 다양한 시나리오(정상, 빈 결과, 예외 등)를 포함
 */
@ExtendWith(MockitoExtension.class)
class UserExternalQueryServiceTest {

	@Mock
	private JpaUserExternalReadRepository userExternalReadRepository;

	@InjectMocks
	private UserExternalQueryService userExternalQueryService;

	private UserEntity mockUser;

	private static final String NOT_FOUND_MSG = "해당 사용자가 존재하지 않습니다.";
	@AfterEach
	void tearDown() {
		userExternalReadRepository.deleteAllInBatch();
	}
	@BeforeEach
	void setUp() {
		mockUser = UserEntity.builder()
			.id(100L)
			.build();
	}

	/* ============================================================================
	   1) searchUserByKeyword(String keyword, Pageable pageable)
	   ============================================================================ */
	@Nested
	@DisplayName("searchUserByKeyword() 테스트")
	class SearchUserByKeywordTest {

		@Test
		@DisplayName("정상 케이스: 검색 결과(Page) 반환, 1개 이상 존재")
		void searchUserByKeyword_Success_WithResults() {
			// given
			String keyword = "testKeyword";
			Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
			Page<UserEntity> mockPage = new PageImpl<>(List.of(mockUser), pageable, 1);

			when(userExternalReadRepository.searchUserByFullText(keyword, pageable))
				.thenReturn(mockPage);

			// when
			Page<UserEntity> result = userExternalQueryService.searchUserByKeyword(keyword, pageable);

			// then
			assertThat(result.getTotalElements()).isEqualTo(1);
			assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
			verify(userExternalReadRepository, times(1))
				.searchUserByFullText(keyword, pageable);
		}

		@Test
		@DisplayName("정상 케이스: 검색 결과가 빈 페이지일 때")
		void searchUserByKeyword_EmptyResult() {
			// given
			String keyword = "noMatch";
			Pageable pageable = PageRequest.of(0, 10);
			Page<UserEntity> emptyPage = Page.empty();

			when(userExternalReadRepository.searchUserByFullText(keyword, pageable))
				.thenReturn(emptyPage);

			// when
			Page<UserEntity> result = userExternalQueryService.searchUserByKeyword(keyword, pageable);

			// then
			assertThat(result).isEmpty();
			verify(userExternalReadRepository, times(1))
				.searchUserByFullText(keyword, pageable);
		}

		@Test
		@DisplayName("DB 오류 등 예외 발생 시 (예시 가정)")
		void searchUserByKeyword_RepositoryThrows() {
			// given
			String keyword = "test";
			Pageable pageable = PageRequest.of(0, 10);
			doThrow(new RuntimeException("DB Error"))
				.when(userExternalReadRepository).searchUserByFullText(keyword, pageable);

			// when & then
			assertThatThrownBy(() -> userExternalQueryService.searchUserByKeyword(keyword, pageable))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB Error");

			verify(userExternalReadRepository, times(1))
				.searchUserByFullText(keyword, pageable);
		}
	}

	/* ============================================================================
	   2) findByName_NameContainingIgnoreCase(String keyword)
	   ============================================================================ */
	@Nested
	@DisplayName("findByName_NameContainingIgnoreCase() 테스트")
	class FindByNameTest {

		@Test
		@DisplayName("정상 케이스: 일부 결과가 존재하는 경우")
		void findByName_Success() {
			// given
			String keyword = "testuser";
			UserEntity user2 = UserEntity.builder().id(200L).build();
			when(userExternalReadRepository.findByNameContainingIgnoreCase(keyword))
				.thenReturn(List.of(mockUser, user2));

			// when
			List<UserEntity> result = userExternalQueryService.findByName_NameContainingIgnoreCase(keyword);

			// then
			assertThat(result).hasSize(2);
			verify(userExternalReadRepository, times(1))
				.findByNameContainingIgnoreCase(keyword);
		}

		@Test
		@DisplayName("정상 케이스: 결과가 비어있는 경우")
		void findByName_Empty() {
			// given
			String keyword = "noMatch";
			when(userExternalReadRepository.findByNameContainingIgnoreCase(keyword))
				.thenReturn(Collections.emptyList());

			// when
			List<UserEntity> result = userExternalQueryService.findByName_NameContainingIgnoreCase(keyword);

			// then
			assertThat(result).isEmpty();
			verify(userExternalReadRepository, times(1))
				.findByNameContainingIgnoreCase(keyword);
		}

		@Test
		@DisplayName("리포지토리에서 예외 발생 시")
		void findByName_RepositoryThrows() {
			// given
			String keyword = "something";
			doThrow(new RuntimeException("DB Error"))
				.when(userExternalReadRepository).findByNameContainingIgnoreCase(keyword);

			// when & then
			assertThatThrownBy(() ->
				userExternalQueryService.findByName_NameContainingIgnoreCase(keyword))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB Error");
			verify(userExternalReadRepository, times(1))
				.findByNameContainingIgnoreCase(keyword);
		}
	}

	/* ============================================================================
	   3) findByIdAndDeletedIsFalse(Long userId)
	   - 존재하면 User 반환, 없으면 UsernameNotFoundException
	   ============================================================================ */
	@Nested
	@DisplayName("findByIdAndDeletedIsFalse() 테스트")
	class FindByIdAndDeletedTest {

		@Test
		@DisplayName("정상 케이스: 유저가 존재하면 반환")
		void findById_Success() {
			Long userId = 123L;
			when(userExternalReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenReturn(Optional.of(mockUser));

			UserEntity result = userExternalQueryService.findByIdAndDeletedIsFalse(userId);

			assertThat(result).isEqualTo(mockUser);
			verify(userExternalReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}

		@Test
		@DisplayName("유저가 존재하지 않으면 UsernameNotFoundException")
		void findById_NotFound() {
			Long userId = 999L;
			when(userExternalReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenReturn(Optional.empty());

			assertThatThrownBy(() -> userExternalQueryService.findByIdAndDeletedIsFalse(userId))
				.isInstanceOf(UserNotFoundException.class)
				.hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

			verify(userExternalReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}

		@Test
		@DisplayName("리포지토리 예외 발생 시")
		void findById_RepositoryThrows() {
			Long userId = 1234L;
			when(userExternalReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenThrow(new RuntimeException("DB Error"));

			assertThatThrownBy(() -> userExternalQueryService.findByIdAndDeletedIsFalse(userId))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB Error");

			verify(userExternalReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}
	}

	/* ============================================================================
	   4) existsByIdAndDeletedIsFalse(Long userId)
	   - true / false / DB Error
	   ============================================================================ */
	@Nested
	@DisplayName("existsByIdAndDeletedIsFalse() 테스트")
	class ExistsByIdAndDeletedTest {

		@Test
		@DisplayName("존재하는 경우 true 반환")
		void exists_True() {
			Long userId = 10L;
			when(userExternalReadRepository.existsByIdAndDeletedIsFalse(userId))
				.thenReturn(true);

			boolean result = userExternalQueryService.existsByIdAndDeletedIsFalse(userId);

			assertThat(result).isTrue();
			verify(userExternalReadRepository, times(1))
				.existsByIdAndDeletedIsFalse(userId);
		}

		@Test
		@DisplayName("존재하지 않을 경우 false 반환")
		void exists_False() {
			Long userId = 20L;
			when(userExternalReadRepository.existsByIdAndDeletedIsFalse(userId))
				.thenReturn(false);

			boolean result = userExternalQueryService.existsByIdAndDeletedIsFalse(userId);

			assertThat(result).isFalse();
			verify(userExternalReadRepository, times(1))
				.existsByIdAndDeletedIsFalse(userId);
		}

		@Test
		@DisplayName("리포지토리에서 예외 발생 시")
		void exists_RepositoryThrows() {
			Long userId = 30L;
			doThrow(new RuntimeException("DB Error"))
				.when(userExternalReadRepository).existsByIdAndDeletedIsFalse(userId);

			assertThatThrownBy(() ->
				userExternalQueryService.existsByIdAndDeletedIsFalse(userId))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB Error");

			verify(userExternalReadRepository, times(1))
				.existsByIdAndDeletedIsFalse(userId);
		}
	}
}
