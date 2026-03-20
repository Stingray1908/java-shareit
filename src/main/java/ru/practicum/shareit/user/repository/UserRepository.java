package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

public interface UserRepository {

    /**
     * Создаёт нового пользователя.
     *
     * @param user объект пользователя для создания
     * @return созданный объект пользователя с присвоенным ID
     * @throws ConflictException если пользователь с таким email уже существует
     */
    User save(User user);

    /**
     * Обновляет существующего пользователя.
     *
     * @param user объект пользователя с обновлёнными данными
     * @return обновлённый объект пользователя
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     * @throws ConflictException      если новый email уже используется другим пользователем
     */
    User update(User user);

    /**
     * Удаляет пользователя по ID.
     *
     * @param id ID пользователя для удаления
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    Optional<User> deleteById(Long id);

    /**
     * Находит пользователя по ID.
     *
     * @param id ID пользователя
     * @return объект пользователя
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    Optional<User> findById(Long id);

    /**
     * Возвращает список всех пользователей.
     *
     * @return список объектов пользователей
     */
    Optional<Collection<User>> findAll();

    boolean isEmailExist(String email);
}
