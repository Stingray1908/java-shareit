package ru.practicum.shareit.user.service;

import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Сервис для бизнес‑логики управления пользователями.
 * Определяет контракт для операций CRUD над сущностями пользователей с учётом бизнес‑правил.
 * <p>
 * Ключевые бизнес‑правила:
 * - Создание пользователя требует уникального email.
 * - Обновление может быть частичным (имя и/или email).
 * - Проверка существования пользователя перед операциями обновления и удаления.
 * - Валидация входных данных на уровне сервиса.
 *
 * <p>
 * Методы сервиса:
 * - create — создаёт нового пользователя, проверяет уникальность email;
 * - update — обновляет данные существующего пользователя, проверяет уникальность нового email;
 * - getById — получает данные пользователя по ID, выбрасывает исключение, если пользователь не найден;
 * - getAll — возвращает список всех пользователей;
 * - delete — удаляет пользователя по ID вместе со связанными данными (вещи, бронирования и т. д.).
 * <p>
 * Исключения:
 * - IllegalArgumentException — при некорректных входных данных (null, пустые поля и т. п.);
 * - ConflictException — при попытке создать/обновить пользователя с существующим email;
 * - NoSuchElementException — если пользователь с указанным ID не найден.
 */
public interface UserService {
    /**
     * Создаёт нового пользователя на основе переданных данных.
     *
     * @param reqDTO DTO с данными для создания пользователя (имя, email)
     * @return DTO с данными созданного пользователя, включая присвоенный ID
     * @throws IllegalArgumentException если name или email равны null,
     *                                  либо если email некорректен
     * @throws ConflictException        если пользователь с таким email уже существует
     */
    UserSendDTO create(UserReqDTO reqDTO);

    /**
     * Обновляет данные существующего пользователя.
     *
     * @param id     идентификатор пользователя, которого нужно обновить
     * @param reqDTO DTO с полями для обновления (имя и/или email)
     * @return DTO с данными обновлённого пользователя
     * @throws IllegalArgumentException если:
     *                                  - id равен null или меньше 1;
     *                                  - не заданы поля для обновления (name и email равны null);
     *                                  - email некорректен (если указан)
     * @throws ConflictException        если новый email уже используется другим пользователем
     * @throws NoSuchElementException   если пользователь с указанным ID не найден
     */
    UserSendDTO update(Long id, UserReqDTO reqDTO);

    /**
     * Получает данные пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return DTO с данными пользователя (ID, имя, email)
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    UserSendDTO getById(Long id);

    /**
     * Внутренний метод для получения сущности пользователя по ID (используется внутри сервиса).
     *
     * @param id идентификатор пользователя
     * @return сущность пользователя из базы данных
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    User getByIdOrThrowInternal(Long id);

    /**
     * Возвращает список всех пользователей в системе.
     *
     * @return коллекция DTO с данными всех пользователей (ID, имя, email)
     */
    Collection<UserSendDTO> getAll();

    /**
     * Удаляет пользователя по его идентификатору вместе со всеми связанными данными
     * (вещами, бронированиями, запросами и т. д.).
     *
     * @param id идентификатор пользователя, которого нужно удалить
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    void delete(Long id);
}
