package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.service.UserService;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSendDTO create(@RequestBody UserReqDTO userReqDTO) {
        log.info("Сервер: получен запрос на создание нового пользователя: {}", userReqDTO);
        UserSendDTO createdUser = userService.create(userReqDTO);
        log.info("Сервер: успешно создан пользователь с ID: {}", createdUser.getId());
        return createdUser;
    }

    @PatchMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserSendDTO update(@PathVariable Long userId,
                              @RequestBody UserReqDTO userReqDTO) {
        log.info("Сервер: получен запрос на обновление пользователя с ID: {}, данные: {}", userId, userReqDTO);
        UserSendDTO updatedUser = userService.update(userId, userReqDTO);
        log.info("Сервер: успешно обновлён пользователь {}", updatedUser);
        return updatedUser;
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserSendDTO getById(@PathVariable Long userId) {
        log.info("Сервер: получен запрос на получение пользователя с ID: {}", userId);
        UserSendDTO user = userService.getById(userId);
        log.info("Сервер: успешно получен пользователь с ID: {}", userId);
        return user;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<UserSendDTO> getAllUsers() {
        log.info("Сервер: получен запрос на получение всех пользователей");
        Collection<UserSendDTO> users = userService.getAll();
        log.info("Сервер: успешно получено {} пользователей", users.size());
        return users;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        log.info("Сервер: получен запрос на удаление пользователя с ID: {}", userId);
        userService.delete(userId);
        log.info("Сервер: успешно удалён пользователь с ID: {}", userId);
    }
}
