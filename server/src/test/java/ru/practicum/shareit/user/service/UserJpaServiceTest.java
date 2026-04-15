package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserJpaServiceTest {

    @Mock
    private UserJpaRepository jpaRepository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserJpaService userService;

    private final Long userId = 1L;
    private final String userName = "Test User";
    private final String userEmail = "test@example.com";

    @Test
    void create_ShouldCreateUser_WhenEmailIsUnique() {
        // Given
        UserReqDTO reqDTO = new UserReqDTO(userName, userEmail);
        User userEntity = new User(userId, userName, userEmail);
        UserSendDTO sendDTO = new UserSendDTO(userId, userName, userEmail);

        when(mapper.toEntity(reqDTO)).thenReturn(userEntity);
        when(jpaRepository.existsByEmail(userEmail)).thenReturn(false);
        when(jpaRepository.save(userEntity)).thenReturn(userEntity);
        when(mapper.toSendDto(userEntity)).thenReturn(sendDTO);

        // When
        UserSendDTO result = userService.create(reqDTO);

        // Then
        assertThat(result).isEqualTo(sendDTO);
        verify(jpaRepository, times(1)).existsByEmail(userEmail);
        verify(jpaRepository, times(1)).save(userEntity);
    }

    @Test
    void create_ShouldThrowConflictException_WhenEmailAlreadyExists() {
        // Given
        UserReqDTO reqDTO = new UserReqDTO(userName, userEmail);
        User userEntity = new User(userId, userName, userEmail);

        when(mapper.toEntity(reqDTO)).thenReturn(userEntity);
        when(jpaRepository.existsByEmail(userEmail)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(reqDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Пользователь с email: " + userEmail + " уже существует");
        verify(jpaRepository, never()).save(any(User.class));
    }

    @Test
    void update_ShouldUpdateUser_WhenValidData() {
        // Given
        UserReqDTO reqDTO = new UserReqDTO("Updated Name", "updated@example.com");
        User existingUser = new User(userId, userName, userEmail);
        User updatedUser = new User(userId, "Updated Name", "updated@example.com");
        UserSendDTO sendDTO = new UserSendDTO(userId, "Updated Name", "updated@example.com");

        when(jpaRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(jpaRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(jpaRepository.save(updatedUser)).thenReturn(updatedUser);
        when(mapper.toSendDto(updatedUser)).thenReturn(sendDTO);

        // When
        UserSendDTO result = userService.update(userId, reqDTO);

        // Then
        assertThat(result).isEqualTo(sendDTO);
        assertThat(existingUser.getName()).isEqualTo("Updated Name");
        assertThat(existingUser.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void update_ShouldThrowNoSuchElementException_WhenUserNotFound() {
        // Given
        UserReqDTO reqDTO = new UserReqDTO("New Name", "new@example.com");

        when(jpaRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.update(userId, reqDTO))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Пользователь с id: " + userId + " не существует");
    }

    @Test
    void update_ShouldThrowIllegalArgumentException_WhenNoFieldsToUpdate() {
        // Given
        UserReqDTO reqDTO = new UserReqDTO(null, null);
        User existingUser = new User(userId, userName, userEmail);

        when(jpaRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.update(userId, reqDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Не заданы имя и email для обновления пользователя");
    }

    @Test
    void getById_ShouldReturnUser_WhenUserExists() {
        // Given
        User user = new User(userId, userName, userEmail);
        UserSendDTO sendDTO = new UserSendDTO(userId, userName, userEmail);

        when(jpaRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.toSendDto(user)).thenReturn(sendDTO);

        // When
        UserSendDTO result = userService.getById(userId);

        // Then
        assertThat(result).isEqualTo(sendDTO);
    }

    @Test
    void getById_ShouldThrowNoSuchElementException_WhenUserNotFound() {
        // Given
        when(jpaRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Пользователь с id: " + userId + " не существует");
    }

    @Test
    void getAll_ShouldReturnAllUsers() {
        // Given
        List<User> users = List.of(
                new User(1L, "User1", "user1@example.com"),
                new User(2L, "User2", "user2@example.com")
        );
        List<UserSendDTO> sendDTOs = List.of(
                new UserSendDTO(1L, "User1", "user1@example.com"),
                new UserSendDTO(2L, "User2", "user2@example.com")
        );

        when(jpaRepository.findAll()).thenReturn(users);
        when(mapper.toSendDto(users.get(0))).thenReturn(sendDTOs.get(0));
        when(mapper.toSendDto(users.get(1))).thenReturn(sendDTOs.get(1));

        // When
        Collection<UserSendDTO> result = userService.getAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsAll(sendDTOs);
    }

    @Test
    void delete_ShouldDeleteUser_WhenUserExists() {
        // When
        userService.delete(userId);

        // Then
        verify(jpaRepository, times(1)).deleteById(userId);
    }
}
