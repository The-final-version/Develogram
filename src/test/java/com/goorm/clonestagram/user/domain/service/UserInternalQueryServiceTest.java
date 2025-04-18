package com.goorm.clonestagram.user.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.repository.UserInternalReadRepository;
import com.goorm.clonestagram.user.domain.repository.UserInternalWriteRepository;

/**
 * UserInternalQueryService 테스트
 * - 모든 메서드(정상/예외) 시나리오를 포괄
 */
@ExtendWith(MockitoExtension.class)
class UserInternalQueryServiceTest {

	@Mock
	private UserInternalReadRepository userReadRepository;

	@Mock
	private UserInternalWriteRepository userWriteRepository;

	@InjectMocks
	private UserInternalQueryService userInternalQueryService;

	private User mockUser;

	@BeforeEach
	void setUp() {
		// 테스트용 Mock User
		mockUser = User.secureBuilder()
			.id(100L)
			.email("aladsnfa@anlkkv.com")
			.password("hashedpassword1!")
			.isHashed(false)
			.name("testuser")
			.build();
	}

	/** =================================================================
	 *  findUserIdByname(String name)
	 *  - repository.findByName(...) → Optional<Long>
	 *  - orElseThrow nameNotFoundException
	 * ================================================================= */
	@Nested
	@DisplayName("findUserIdByname() 테스트")
	class FindUserIdBynameTest {

		@Test
		@DisplayName("정상 케이스: 존재하는 name → userId 반환")
		void findUserIdByname_Success() {
			// given
			String name = "testuser";
			when(userReadRepository.findByName(name)).thenReturn(Optional.of(999L));

			// when
			Long foundId = userInternalQueryService.findUserIdByname(name);

			// then
			assertThat(foundId).isEqualTo(999L);
			verify(userReadRepository, times(1)).findByName(name);
		}

		@Test
		@DisplayName("존재하지 않는 name → nameNotFoundException")
		void findUserIdByname_NotFound() {
			// given
			String name = "notExist";
			when(userReadRepository.findByName(name)).thenReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> userInternalQueryService.findUserIdByname(name))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
			verify(userReadRepository, times(1)).findByName(name);
		}
	}

	/** =================================================================
	 *  findByEmail(String email)
	 *  - repository.findByEmail(email) → null or User
	 *  - Optional.ofNullable(...) -> orElseThrow nameNotFoundException
	 * ================================================================= */
	@Nested
	@DisplayName("findByEmail() 테스트")
	class FindByEmailTest {

		@Test
		@DisplayName("정상 케이스: DB에서 유저를 찾으면 해당 User 반환")
		void findByEmail_Success() {
			// given
			String email = "test@example.com";
			when(userReadRepository.findByEmail(email)).thenReturn(mockUser);

			// when
			User result = userInternalQueryService.findByEmail(email);

			// then
			assertThat(result).isEqualTo(mockUser);
			verify(userReadRepository, times(1)).findByEmail(email);
		}

		@Test
		@DisplayName("유저가 없으면 nameNotFoundException")
		void findByEmail_NotFound() {
			// given
			String email = "noexist@example.com";
			when(userReadRepository.findByEmail(email)).thenReturn(null);

			// when & then
			assertThatThrownBy(() -> userInternalQueryService.findByEmail(email))
				.isInstanceOf(BusinessException.class);
			verify(userReadRepository, times(1)).findByEmail(email);
		}
	}

	/** =================================================================
	 *  saveUser(User user)
	 *  - 단순 repository.save(user)
	 * ================================================================= */
	@Nested
	@DisplayName("saveUser() 테스트")
	class SaveUserTest {

		/*@Test
		@DisplayName("정상 케이스: userWriteRepository.save()가 호출됨")
		void saveUser_Success() {
			// given
			User userToSave = mockUser;

			// stubbing
			doNothing().when(userWriteRepository).save(userToSave);

			// when
			userInternalQueryService.saveUser(userToSave);

			// then
			verify(userWriteRepository, times(1)).save(userToSave);
		}*/

		@Test
		@DisplayName("save 시 Repository가 예외를 던지는 경우 - DB에러 등 가정")
		void saveUser_RepositoryThrows() {
			// given
			User userToSave = mockUser;
			doThrow(new RuntimeException("DB Error")).when(userWriteRepository).save(userToSave);

			// when & then
			assertThatThrownBy(() -> userInternalQueryService.saveUser(userToSave))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB Error");
			verify(userWriteRepository, times(1)).save(userToSave);
		}
	}

	/** =================================================================
	 *  deleteUserId(Long userId)
	 *  - repository.deleteById(userId)
	 * ================================================================= */
	@Nested
	@DisplayName("deleteUserId() 테스트")
	class DeleteUserIdTest {

		@Test
		@DisplayName("정상 케이스: userWriteRepository.deleteById()가 호출됨")
		void deleteUserId_Success() {
			// given
			Long userId = 10L;
			doNothing().when(userWriteRepository).deleteById(userId);

			// when
			userInternalQueryService.deleteUserId(userId);

			// then
			verify(userWriteRepository, times(1)).deleteById(userId);
		}

		@Test
		@DisplayName("delete 시 Repository가 예외를 던지는 경우 - DB에러 등 가정")
		void deleteUserId_RepositoryThrows() {
			// given
			Long userId = 10L;
			doThrow(new RuntimeException("DB Error")).when(userWriteRepository).deleteById(userId);

			// when & then
			assertThatThrownBy(() -> userInternalQueryService.deleteUserId(userId))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB Error");
			verify(userWriteRepository, times(1)).deleteById(userId);
		}
	}

	/** =================================================================
	 *  existsByEmail(String email)
	 *  - repository.existsByEmail(UserEmail)
	 * =================================================================
	@Nested
	@DisplayName("existsByEmail() 테스트")
	class ExistsByEmailTest {

		@Test
		@DisplayName("존재할 경우 - true 반환")
		void existsByEmail_True() {
			// given
			String email = "exists@test.com";
			when(userReadRepository.existsByEmail(new UserEmail(email))).thenReturn(true);

			// when
			boolean result = userInternalQueryService.existsByEmail(email);

			// then
			assertThat(result).isTrue();
			verify(userReadRepository, times(1)).existsByEmail(new UserEmail(email));
		}

		@Test
		@DisplayName("존재하지 않을 경우 - false 반환")
		void existsByEmail_False() {
			// given
			String email = "noexist@test.com";
			when(userReadRepository.existsByEmail(new UserEmail(email))).thenReturn(false);

			// when
			boolean result = userInternalQueryService.existsByEmail(email);

			// then
			assertThat(result).isFalse();
			verify(userReadRepository, times(1)).existsByEmail(new UserEmail(email));
		}
	}*/

	/** =================================================================
	 *  findUserById(Long userId)
	 *  - repository.findByIdAndDeletedIsFalse(userId) -> or nameNotFoundException
	 * ================================================================= */
	@Nested
	@DisplayName("findUserById() 테스트")
	class FindUserByIdTest {

		@Test
		@DisplayName("정상 케이스: user 반환")
		void findUserById_Success() {
			Long userId = 123L;
			when(userReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenReturn(Optional.of(mockUser));

			User result = userInternalQueryService.findUserById(userId);

			assertThat(result).isEqualTo(mockUser);
			verify(userReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}

		@Test
		@DisplayName("존재하지 않을 경우 nameNotFoundException")
		void findUserById_NotFound() {
			Long userId = 404L;
			when(userReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenReturn(Optional.empty());

			assertThatThrownBy(() -> userInternalQueryService.findUserById(userId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
			verify(userReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}
	}

	/** =================================================================
	 *  findByIdAndDeletedIsFalse(Long userId)
	 *  - (실제 로직이 findUserById와 동일)
	 * ================================================================= */
	@Nested
	@DisplayName("findByIdAndDeletedIsFalse() 테스트")
	class FindByIdAndDeletedIsFalseTest {

		@Test
		@DisplayName("정상 케이스: user 반환")
		void findByIdAndDeletedIsFalse_Success() {
			Long userId = 222L;
			when(userReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenReturn(Optional.of(mockUser));

			User result = userInternalQueryService.findByIdAndDeletedIsFalse(userId);

			assertThat(result).isEqualTo(mockUser);
			verify(userReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}

		@Test
		@DisplayName("존재하지 않을 경우 - nameNotFoundException")
		void findByIdAndDeletedIsFalse_NotFound() {
			Long userId = 999L;
			when(userReadRepository.findByIdAndDeletedIsFalse(userId))
				.thenReturn(Optional.empty());

			assertThatThrownBy(() -> userInternalQueryService.findByIdAndDeletedIsFalse(userId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
			verify(userReadRepository, times(1))
				.findByIdAndDeletedIsFalse(userId);
		}
	}
}
