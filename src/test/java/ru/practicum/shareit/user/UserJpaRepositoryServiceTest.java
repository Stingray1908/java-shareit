package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = "ru.practicum.shareit.user")
class UserJpaRepositoryServiceTest {

    @Autowired
    private UserJPARepository userRepository;

    private final UserMapper userMapper = new UserMapper();

    private UserJPAService userService;

    // Фиксированные тестовые данные
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_USER_EMAIL = "test@example.com";

    private static final String UPDATED_NAME = "Updated Name";
    private static final String UPDATED_EMAIL = "updated@example.com";
    private static final String DUPLICATE_EMAIL = "duplicate@example.com";

    private Long firstUserId;
    private Long secondUserId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userService = new UserJPAService(userRepository, userMapper);

        // Создаём базовых пользователей для тестов
        firstUserId = addNewUserToDB(TEST_USER_NAME, TEST_USER_EMAIL).getId();
        secondUserId = addNewUserToDB("Second User", "second@example.com").getId();
    }

    // Вспомогательные методы
    private UserReqDTO createUserRequest(String name, String email) {
        return new UserReqDTO(name, email);
    }

    private UserSendDTO addNewUserToDB(String name, String email) {
        UserReqDTO userRequest = createUserRequest(name, email);
        return userService.create(userRequest);
    }

    private void assertUserEquals(UserSendDTO actual, Long expectedId, String expectedName, String expectedEmail) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expectedId);
        assertThat(actual.getName()).isEqualTo(expectedName);
        assertThat(actual.getEmail()).isEqualTo(expectedEmail);
    }

    private void assertUserInDBEquals(Long userId, String expectedName, String expectedEmail) {
        User userInDb = userRepository.findById(userId)
                .orElseThrow(() -> new AssertionError("Пользователь не найден в репозитории после операции"));
        assertThat(userInDb.getName()).isEqualTo(expectedName);
        assertThat(userInDb.getEmail()).isEqualTo(expectedEmail);
    }

    // Тесты

    @Test
    void testSaveUser_Success() {
        // Arrange
        String name = TEST_USER_NAME + "_new";
        String email = "new_" + TEST_USER_EMAIL;

        // Act
        UserSendDTO savedUser = addNewUserToDB(name, email);

        // Assert
        assertUserEquals(savedUser, savedUser.getId(), name, email);
        assertUserInDBEquals(savedUser.getId(), name, email);
    }

    @Test
    void testSaveUser_DuplicateEmail_ThrowsConflictException() {
        // Arrange: используем фиксированный email для дублирования
        addNewUserToDB("First User", DUPLICATE_EMAIL);
        UserReqDTO secondUser = createUserRequest("Second User", DUPLICATE_EMAIL);

        // Act & Assert
        assertThatThrownBy(() -> userService.create(secondUser))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(DUPLICATE_EMAIL)
                .hasMessageContaining("уже существует");
    }

    @Test
    void testGetById_ExistingUser_ReturnsCorrectUser() {
        // Act
        UserSendDTO foundUser = userService.getById(firstUserId);

        // Assert
        assertUserEquals(foundUser, firstUserId, TEST_USER_NAME, TEST_USER_EMAIL);
        assertUserInDBEquals(firstUserId, TEST_USER_NAME, TEST_USER_EMAIL);
    }

    @Test
    void testGetById_NonExistentUser_ThrowsNoSuchElementException() {
        // Arrange: используем заведомо несуществующий ID
        Long nonExistentId = firstUserId + 100L;

        // Act & Assert
        assertThatThrownBy(() -> userService.getById(nonExistentId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Пользователь с id: " + nonExistentId + " не существует");
    }

    @Test
    void testUpdate_NullNameAndEmail_ThrowsIllegalArgumentException() {
        // Arrange
        UserReqDTO updateRequest = createUserRequest(null, null);

        // Act & Assert
        assertThatThrownBy(() -> userService.update(firstUserId, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Не заданы имя и email для обновления пользователя");
    }

    @Test
    void testUpdate_OnlyName_UpdatesNameSuccessfully() {
        // Arrange
        UserReqDTO updateRequest = createUserRequest(UPDATED_NAME, null);

        // Act
        UserSendDTO updatedUser = userService.update(firstUserId, updateRequest);

        // Assert
        assertUserEquals(updatedUser, firstUserId, UPDATED_NAME, TEST_USER_EMAIL);
        assertUserInDBEquals(firstUserId, UPDATED_NAME, TEST_USER_EMAIL);
    }

    @Test
    void testUpdate_OnlyEmail_UpdatesEmailSuccessfully() {
        // Arrange
        UserReqDTO updateRequest = createUserRequest(null, UPDATED_EMAIL);

        // Act
        UserSendDTO updatedUser = userService.update(firstUserId, updateRequest);

        // Assert
        assertUserEquals(updatedUser, firstUserId, TEST_USER_NAME, UPDATED_EMAIL);
        assertUserInDBEquals(firstUserId, TEST_USER_NAME, UPDATED_EMAIL);
    }

    @Test
    void testUpdate_BothFields_UpdatesBothSuccessfully() {
        // Arrange
        UserReqDTO updateRequest = createUserRequest(UPDATED_NAME, UPDATED_EMAIL);

        // Act
        UserSendDTO updatedUser = userService.update(firstUserId, updateRequest);

        // Assert
        assertUserEquals(updatedUser, firstUserId, UPDATED_NAME, UPDATED_EMAIL);
        assertUserInDBEquals(firstUserId, UPDATED_NAME, UPDATED_EMAIL);
    }

    @Test
    void testUpdate_EmailAlreadyExists_ThrowsConflictException() {
        // Arrange: пытаемся обновить email второго пользователя на email первого
        UserReqDTO updateRequest = createUserRequest(null, TEST_USER_EMAIL);

        // Act & Assert
        assertThatThrownBy(() -> userService.update(secondUserId, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(TEST_USER_EMAIL)
                .hasMessageContaining("уже существует");
    }

    @Test
    void testGetAll_EmptyList_WhenNoUsers() {
        // Arrange: убеждаемся, что в БД нет пользователей
        userRepository.deleteAll();

        // Act
        Collection<UserSendDTO> users = userService.getAll();

        // Assert
        assertThat(users).isEmpty();
    }

        @Test
        void testGetAll_ReturnsUsersList_WhenUsersExist() {
            userRepository.deleteAll();

            // Arrange: создаём нескольких пользователей
            UserSendDTO user1 = addNewUserToDB("User One", "one@example.com");
            UserSendDTO user2 = addNewUserToDB("User Two", "two@example.com");
            UserSendDTO user3 = addNewUserToDB("User Three", "three@example.com");

            // Act
            Collection<UserSendDTO> users = userService.getAll();

            // Assert: проверяем размер и содержимое списка
            assertThat(users).isNotEmpty();
            assertThat(users).hasSize(3);

            // Проверяем, что все пользователи присутствуют в результате
            assertThat(users)
                    .extracting("id", "name", "email")
                    .contains(
                            tuple(user1.getId(), user1.getName(), user1.getEmail()),
                            tuple(user2.getId(), user2.getName(), user2.getEmail()),
                            tuple(user3.getId(), user3.getName(), user3.getEmail())
                    );
        }

    @Test
    void testDelete_ExistingUser_DeletesSuccessfully() {
        // Arrange: создаём пользователя для удаления
        UserSendDTO createdUser = addNewUserToDB("User to Delete", "delete@example.com");
        Long userIdToDelete = createdUser.getId();

        // Проверяем, что пользователь существует до удаления
        assertThat(userService.getById(userIdToDelete)).isNotNull();

        // Act: выполняем удаление
        userService.delete(userIdToDelete);

        // Assert: проверяем, что пользователь больше не существует
        assertThatThrownBy(() -> userService.getById(userIdToDelete))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Пользователь с id: " + userIdToDelete + " не существует");

        // Дополнительная проверка через репозиторий — убеждаемся, что запись удалена из БД
        assertThat(userRepository.findById(userIdToDelete)).isEmpty();
    }
}
