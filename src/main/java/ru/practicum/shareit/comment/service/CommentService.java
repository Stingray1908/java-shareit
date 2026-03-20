package ru.practicum.shareit.comment.service;

import ru.practicum.shareit.comment.dto.CommentReqDto;
import ru.practicum.shareit.comment.dto.CommentSendDto;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Сервис для управления комментариями (отзывами) о предметах в системе.
 */
public interface CommentService {

    /**
     * Создаёт новый комментарий (отзыв) о предмете.
     *
     * @param reqDto   DTO с текстом комментария и дополнительными данными
     * @param bookerId идентификатор пользователя — автора комментария
     * @param itemId   идентификатор предмета, к которому относится комментарий
     * @return DTO с данными созданного комментария
     * @throws IllegalArgumentException если:
     *                                  - пользователь не имеет завершённого бронирования (COMPLETED) для этой вещи;
     *                                  - пользователь уже оставил отзыв на эту вещь
     * @throws NoSuchElementException   если бронирование или предмет не найдены
     */
    CommentSendDto createComment(CommentReqDto reqDto, Long bookerId, Long itemId);

    /**
     * Обновляет существующий комментарий.
     *
     * @param commentId идентификатор комментария, который нужно обновить
     * @param reqDto    DTO с обновлённым текстом комментария и данными
     * @param bookerId  идентификатор пользователя — автора комментария (для проверки прав доступа)
     * @return DTO с данными обновлённого комментария
     * @throws NoSuchElementException если комментарий с указанным ID не найден
     * @throws SecurityException      если пользователь не является автором комментария
     */
    CommentSendDto updateComment(Long commentId, CommentReqDto reqDto, Long bookerId);

    /**
     * Удаляет комментарий по его идентификатору.
     *
     * @param commentId идентификатор комментария для удаления
     * @param bookerId  идентификатор пользователя — автора комментария (для проверки прав доступа)
     * @throws NoSuchElementException если комментарий с указанным ID не найден
     * @throws SecurityException      если пользователь не является автором комментария
     */
    void deleteComment(Long commentId, Long bookerId);

    /**
     * Получает коллекцию комментариев, оставленных для указанного предмета.
     *
     * @param itemId идентификатор предмета
     * @return коллекция DTO с комментариями к предмету
     */
    Collection<CommentSendDto> getCommentsByItemId(Long itemId);
}
