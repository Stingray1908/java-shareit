package ru.practicum.shareit.user;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.groups.OnCreate;
import ru.practicum.shareit.common.groups.OnUpdate;
import ru.practicum.shareit.user.dto.UserReqDTO;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserClient userClient;

    @PostMapping
    public ResponseEntity<Object> create(@Validated(OnCreate.class)
                                         @RequestBody UserReqDTO userReqDTO) {
        log.info("Ворота: получен запрос на создание нового пользователя: {}", userReqDTO);
        ResponseEntity<Object> result = userClient.create(userReqDTO);
        log.info("Ворота: ответ от сервера получен для создания пользователя");
        return result;
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<Object> update(
            @PathVariable @Positive(message = "ID пользователя должен быть положительным числом") Long userId,
            @Validated(OnUpdate.class) @RequestBody UserReqDTO userReqDTO) {
        log.info("Ворота: получен запрос на обновление пользователя с ID: {}, данные: {}", userId, userReqDTO);
        ResponseEntity<Object> result = userClient.update(userId, userReqDTO);
        log.info("Ворота: ответ от сервера получен для обновления пользователя с ID: {}", userId);
        return result;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getById(
            @PathVariable @Positive(message = "ID пользователя должен быть положительным числом") Long userId) {
        log.info("Ворота: получен запрос на получение пользователя с ID: {}", userId);
        ResponseEntity<Object> result = userClient.getById(userId);
        log.info("Ворота: информация о пользователе с ID: {} успешно получена", userId);
        return result;
    }

    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        log.info("Ворота: получен запрос на получение всех пользователей");
        ResponseEntity<Object> result = userClient.getAllUsers();
        log.info("Ворота: ответ от сервера получен для получения всех пользователей");
        return result;
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> delete(
            @PathVariable @Positive(message = "ID пользователя должен быть положительным числом") Long userId) {
        log.info("Ворота: получен запрос на удаление пользователя с ID: {}", userId);
        ResponseEntity<Object> result = userClient.delete(userId);
        log.info("Ворота: пользователь с ID: {} успешно удалён", userId);
        return result;
    }
}
