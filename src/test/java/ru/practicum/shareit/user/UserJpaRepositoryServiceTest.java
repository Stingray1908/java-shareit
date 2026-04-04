package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;

import java.util.Collection;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

class UserJpaRepositoryServiceTest {

    @Autowired
    private UserJPARepository userRepository;

    @Autowired
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
        cleanDatabase();
        firstUserId = createUser(TEST_USER_NAME, TEST_USER_EMAIL);
        secondUserId = createUser("Second User", "second@example.com");
    }

    private void cleanDatabase() {
        userRepository.deleteAll();
    }

    private Long createUser(String name, String email) {
        UserReqDTO userRequest = new UserReqDTO(name, email);
        return userService.create(userRequest).getId();
    }

    private UserReqDTO createUpdateRequest(String name, String email) {
        return new UserReqDTO(name, email);
    }

    private void assertUserFields(UserSendDTO actual, Long expectedId, String expectedName, String expectedEmail) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expectedId);
        assertThat(actual.getName()).isEqualTo(expectedName);
        assertThat(actual.getEmail()).isEqualTo(expectedEmail);
    }

    private void assertUserInDatabase(Long userId, String expectedName, String expectedEmail) {
        var userInDb = userRepository.findById(userId)
                .orElseThrow(() -> new AssertionError("Пользователь не найден в репозитории после операции"));
        assertThat(userInDb.getName()).isEqualTo(expectedName);
        assertThat(userInDb.getEmail()).isEqualTo(expectedEmail);
    }

    private void assertUserNotFound(Long userId) {
        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Пользователь с id: " + userId + " не существует");
    }

    @Test
    void create_ShouldSaveUserWithValidData_WhenEmailIsUnique() {
        // Given
        String newName = TEST_USER_NAME + "_new";
        String newEmail = "new_" + TEST_USER_EMAIL;

        // When
        UserSendDTO savedUser = userService.create(new UserReqDTO(newName, newEmail));

        // Then
        assertUserFields(savedUser, savedUser.getId(), newName, newEmail);
        assertUserInDatabase(savedUser.getId(), newName, newEmail);
    }

    @Test
    void create_ShouldThrowConflictException_WhenEmailAlreadyExists() {
        // Given
        createUser("First User", DUPLICATE_EMAIL);
        UserReqDTO duplicateUser = new UserReqDTO("Second User", DUPLICATE_EMAIL);

        // When & Then
        assertThatThrownBy(() -> userService.create(duplicateUser))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(DUPLICATE_EMAIL)
                .hasMessageContaining("уже существует");
    }

    @Test
    void getById_ShouldReturnUser_WhenUserExists() {
        // When
        UserSendDTO foundUser = userService.getById(firstUserId);

        // Then
        assertUserFields(foundUser, firstUserId, TEST_USER_NAME, TEST_USER_EMAIL);
        assertUserInDatabase(firstUserId, TEST_USER_NAME, TEST_USER_EMAIL);
    }

    @Test
    void getById_ShouldThrowNoSuchElementException_WhenUserDoesNotExist() {
        // Given
        Long nonExistentId = firstUserId + 100L;

        // When & Then
        assertUserNotFound(nonExistentId);
    }

    @Test
    void update_ShouldThrowIllegalArgumentException_WhenBothFieldsAreNull() {
        // Given
        UserReqDTO updateRequest = createUpdateRequest(null, null);

        // When & Then
        assertThatThrownBy(() -> userService.update(firstUserId, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Не заданы имя и email для обновления пользователя");
    }

    @Test
    void update_ShouldUpdateNameSuccessfully_WhenOnlyNameProvided() {
        // Given
        UserReqDTO updateRequest = createUpdateRequest(UPDATED_NAME, null);

        // When
        UserSendDTO updatedUser = userService.update(firstUserId, updateRequest);

        // Then
        assertUserFields(updatedUser, firstUserId, UPDATED_NAME, TEST_USER_EMAIL);
        assertUserInDatabase(firstUserId, UPDATED_NAME, TEST_USER_EMAIL);
    }

    @Test
    void update_ShouldUpdateEmailSuccessfully_WhenOnlyEmailProvided() {
        // Given
        UserReqDTO updateRequest = createUpdateRequest(null, UPDATED_EMAIL);

        // When
        UserSendDTO updatedUser = userService.update(firstUserId, updateRequest);

        // Then
        assertUserFields(updatedUser, firstUserId, TEST_USER_NAME, UPDATED_EMAIL);
        assertUserInDatabase(firstUserId, TEST_USER_NAME, UPDATED_EMAIL);
    }

    @Test
    void update_ShouldUpdateBothFieldsSuccessfully_WhenBothProvided() {
        // Given
        UserReqDTO updateRequest = createUpdateRequest(UPDATED_NAME, UPDATED_EMAIL);

        // When
        UserSendDTO updatedUser = userService.update(firstUserId, updateRequest);

        // Then
        assertUserFields(updatedUser, firstUserId, UPDATED_NAME, UPDATED_EMAIL);
        assertUserInDatabase(firstUserId, UPDATED_NAME, UPDATED_EMAIL);
    }

    @Test
    void update_ShouldThrowConflictException_WhenEmailAlreadyExists() {
        // Given
        UserReqDTO updateRequest = createUpdateRequest(null, TEST_USER_EMAIL);

        // When & Then
        assertThatThrownBy(() -> userService.update(secondUserId, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(TEST_USER_EMAIL)
                .hasMessageContaining("уже существует");
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoUsers() {
        // Given
        cleanDatabase();

        // When
        Collection<UserSendDTO> users = userService.getAll();

        // Then
        assertThat(users).isEmpty();
    }

    @Test
    void getAll_ShouldReturnUsersList_WhenUsersExist() {
        // Given
        cleanDatabase();
        UserSendDTO user1 = userService.create(new UserReqDTO("User One", "one@example.com"));
        UserSendDTO user2 = userService.create(new UserReqDTO("User Two", "two@example.com"));
        UserSendDTO user3 = userService.create(new UserReqDTO("User Three", "three@example.com"));

        // When
        Collection<UserSendDTO> users = userService.getAll();

        // Then
        assertThat(users).isNotEmpty();
        assertThat(users).hasSize(3);

        assertThat(users)
                .extracting("id", "name", "email")
                .contains(
                        tuple(user1.getId(), user1.getName(), user1.getEmail()),
                        tuple(user2.getId(), user2.getName(), user2.getEmail()),
                        tuple(user3.getId(), user3.getName(), user3.getEmail())
                );
    }

    @Test
    void delete_ShouldDeleteUserSuccessfully_WhenUserExists() {
        // Given
        UserSendDTO createdUser = userService.create(new UserReqDTO("User to Delete", "delete@example.com"));
        Long userIdToDelete = createdUser.getId();

        // Verify user exists before deletion
        assertThat(userService.getById(userIdToDelete)).isNotNull();

        // When
        userService.delete(userIdToDelete);

        // Then
        assertUserNotFound(userIdToDelete);
        assertThat(userRepository.findById(userIdToDelete)).isEmpty();
    }
}

