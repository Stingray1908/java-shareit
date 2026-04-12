package ru.practicum.shareit.user;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.groups.OnCreate;
import ru.practicum.shareit.common.groups.OnUpdate;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.service.UserService;

import java.util.Collection;

/**
 * REST‑контроллер для управления учётными записями пользователей.
 * Предоставляет полный набор API‑методов для выполнения операций CRUD (создание, чтение, обновление, удаление)
 * над сущностями пользователей.
 *
 * Основные эндпоинты:
 * - POST /users — создание нового пользователя (требуется уникальное имя и email);
 * - PATCH /users/{userId} — частичное обновление данных пользователя (имя и/или email);
 * - GET /users/{userId} — получение данных конкретного пользователя по ID;
 * - GET /users — получение списка всех пользователей в системе;
 * - DELETE /users/{userId} — удаление пользователя по ID.
 *
 * <p>
 * Основные правила бизнес‑логики:
 * - При создании пользователя обязательно указание имени и email.
 * - Email должен быть уникальным в системе — попытка создать пользователя с существующим email
 *   приводит к ошибке ConflictException (409).
 * - Обновление данных пользователя частичное (PATCH): можно изменить имя и/или email.
 * - При обновлении email проверяется его уникальность (не должен совпадать с email других пользователей).
 * - Если при обновлении не указаны ни имя, ни email, выбрасывается IllegalArgumentException (400).
 * - Все операции, требующие ID пользователя, проверяют его существование в системе.
 *   Если пользователь не найден, выбрасывается NoSuchElementException (404).
 * - ID пользователя должен быть положительным числом во всех запросах, где он используется.
 * - Удаление пользователя возвращает статус NO_CONTENT (204) при успешном выполнении.
 * <p>
 * Логирование:
 * - Каждый запрос логируется с указанием параметров и результата операции.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * Создаёт нового пользователя.
     * Проверяет уникальность email. При дублировании выбрасывает ConflictException.
     * Возвращает UserSendDTO с присвоенным ID.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSendDTO create(@Validated(OnCreate.class)
                              @RequestBody UserReqDTO userReqDTO) {

        log.info("Получен запрос на создание нового пользователя: {}", userReqDTO);
        UserSendDTO createdUser = userService.create(userReqDTO);
        log.info("Успешно создан пользователь с ID: {}", createdUser.getId());
        return createdUser;
    }

    /**
     * Частично обновляет данные пользователя (PATCH).
     * Позволяет изменить имя и/или email. Для email выполняется проверка уникальности.
     * ID пользователя передаётся в пути запроса.
     */
    @PatchMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserSendDTO update(@PathVariable
                              @Positive(message = "ID пользователя должен быть положительным числом")
                              Long userId,
                              @RequestBody
                              @Validated(OnUpdate.class)
                              UserReqDTO userReqDTO) {
        log.info("Получен запрос на обновление пользователя с ID: {}, данные: {}", userId, userReqDTO);
        UserSendDTO updatedUser = userService.update(userId, userReqDTO);
        log.info("Успешно обновлён пользователь {}", updatedUser);
        return updatedUser;
    }

    /**
     * Возвращает данные пользователя по ID.
     * Если пользователь не найден, выбрасывает NoSuchElementException.
     */
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserSendDTO getById(@PathVariable
                               @Positive(message = "ID пользователя должен быть положительным числом") Long userId) {

        log.info("Получен запрос на получение пользователя с ID: {}", userId);
        UserSendDTO user = userService.getById(userId);
        log.info("Успешно получен пользователь с ID: {}", userId);

        return user;
    }

    /**
     * Возвращает список всех пользователей в системе.
     * При отсутствии пользователей возвращает пустой список.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<UserSendDTO> getAllUsers() {

        log.info("Получен запрос на получение всех пользователей");
        Collection<UserSendDTO> users = userService.getAll();
        log.info("Успешно получено {} пользователей", users.size());
        return users;
    }

    /**
     * Удаляет пользователя по ID, все его вещи
     * Если пользователь не найден, выбрасывает NoSuchElementException.
     * Статус ответа — NO_CONTENT (204).
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable
                       @Positive(message = "ID пользователя должен быть положительным числом")
                       Long userId) {

        log.info("Получен запрос на удаление пользователя с ID: {}", userId);
        userService.delete(userId);
        log.info("Успешно удалён пользователь с ID: {}", userId);
    }
}
