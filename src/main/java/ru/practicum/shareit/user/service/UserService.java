package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;

import java.util.Collection;

public interface UserService {

    /**
     * Создаёт нового пользователя на основе переданных данных.
     *
     * @param reqDTO DTO с данными для создания пользователя
     * @return DTO с данными созданного пользователя
     * @throws IllegalArgumentException если name или email равны null,
     *                                  либо если email некорректен
     */
    UserSendDTO create(UserReqDTO reqDTO);

    /**
     * Обновляет данные существующего пользователя.
     *
     * @param reqDTO DTO с данными для обновления пользователя
     * @return DTO с данными обновлённого пользователя
     * @throws IllegalArgumentException если:
     *                                  - id равен null или меньше 1;
     *                                  - не заданы поля для обновления (name и email равны null);
     *                                  - email некорректен (если указан)
     */
    UserSendDTO update(Long id, UserReqDTO reqDTO);

    /**
     * Получает данные пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return DTO с данными пользователя
     */
    UserSendDTO getById(Long id);

    User getByIdInternal(Long id);

    /**
     * Возвращает список всех пользователей.
     *
     * @return список DTO с данными всех пользователей
     */
    Collection<UserSendDTO> getAll();

    /**
     * Удаляет пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя, которого нужно удалить
     */
    void delete(Long id);
}
