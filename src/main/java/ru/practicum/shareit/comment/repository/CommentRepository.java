package ru.practicum.shareit.comment.repository;

import ru.practicum.shareit.comment.Comment;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с комментариями (отзывами) о предметах в системе.
 */
public interface CommentRepository {

    /**
     * Сохраняет новый комментарий в хранилище.
     *
     * @param comment сущность комментария для сохранения
     * @return сохранённая сущность комментария с присвоенным ID
     */
    Comment addComment(Comment comment);

    /**
     * Находит комментарий по его идентификатору.
     *
     * @param commentId идентификатор комментария
     * @return Optional с сущностью комментария, если найден, или пустой Optional
     */
    Optional<Comment> getComment(Long commentId);

    /**
     * Удаляет комментарий из хранилища по его идентификатору.
     *
     * @param commentId идентификатор комментария для удаления
     */
    void deleteComment(Long commentId);

    /**
     * Возвращает список всех комментариев, оставленных для указанного предмета.
     *
     * @param itemId идентификатор предмета
     * @return список сущностей комментариев к предмету
     */
    List<Comment> getCommentsByItemId(Long itemId);

    /**
     * Проверяет, существует ли комментарий от указанного пользователя для указанного предмета.
     * Используется для предотвращения дублирования отзывов от одного пользователя на один предмет.
     *
     * @param bookerId идентификатор пользователя — автора комментария
     * @param itemId   идентификатор предмета
     * @return true, если такой комментарий уже существует; false — в противном случае
     */
    boolean existsByBookerAndItem(Long bookerId, Long itemId);
}
