package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserJPARepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserJPARepositoryTest {

    @Autowired
    private UserJPARepository userRepository;

    @Test
    void testSaveUser_Success() {
        // Arrange: создаём тестового пользователя
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");

        // Act: сохраняем в БД через репозиторий
        User savedUser = userRepository.save(user);

        // Assert: проверяем, что пользователь сохранён корректно
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isEqualTo(1L); // ID должен быть сгенерирован БД
        assertThat(savedUser.getName()).isEqualTo("Test User");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }
}
