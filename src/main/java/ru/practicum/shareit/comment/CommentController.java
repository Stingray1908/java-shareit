/*package ru.practicum.shareit.comment;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.comment.dto.CommentReqDto;
import ru.practicum.shareit.comment.dto.CommentSendDto;
import ru.practicum.shareit.comment.service.CommentService;
import ru.practicum.shareit.common.constants.HttpHeader;

import java.util.Collection;

/**
 * REST‑контроллер для управления отзывами (комментариями) о вещах в системе.
 * Предоставляет полный набор API‑методов для выполнения операций CRUD (создание, чтение, обновление, удаление)
 * над сущностями отзывов с учётом бизнес‑правил платформы.
 * <p>
 * Основные правила бизнес‑логики:
 * - Создание отзыва:
 * - Пользователь должен иметь завершённое бронирование (статус COMPLETED) данной вещи.
 * - Каждый пользователь может оставить только один отзыв на конкретную вещь.
 * - Отзыв может оставить только пользователь, который реально пользовался вещью (подтверждается через историю бронирований).
 * - При нарушении условий возвращается ошибка 403 Forbidden.
 * - Обновление отзыва:
 * - Обновить может только автор отзыва (сравнение ID пользователя с автором комментария).
 * - Отзыв должен существовать в системе (иначе 404 Not Found).
 * - Попытка обновления чужого отзыва приводит к ошибке доступа (403 Forbidden).
 * - Удаление отзыва:
 * - Удалить может только автор отзыва.
 * - Отзыв должен существовать в системе (иначе 404 Not Found).
 * - Попытка удаления чужого отзыва приводит к ошибке доступа (403 Forbidden).
 * - Просмотр отзывов:
 * - Доступен всем пользователям без авторизации.
 * - Возвращает все отзывы для конкретной вещи в порядке создания (от старых к новым).
 * - Если отзывов нет, возвращает пустой список (200 OK).
 * <p>
 * Общие требования к данным:
 * - ID пользователя (X-User-Id) должен быть положительным числом.
 * - ID вещи (itemId) должен быть положительным числом.
 * - ID отзыва (commentId) должен быть положительным числом.
 * - Текст отзыва не может быть пустым и не должен превышать 1000 символов.
 */
/*@Slf4j
@Validated
//@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Добавление нового отзыва на вещь.
     * <p>
     * Условия успешного выполнения:
     * - Пользователь имеет завершённое бронирование (COMPLETED) этой вещи.
     * - Пользователь ещё не оставлял отзыв на эту вещь.
     * - Пользователь является арендатором вещи (подтверждено историей бронирований).
     */
    /*@PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentSendDto createComment(
            @Validated
            @RequestBody CommentReqDto reqDto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_USER_ID) Long bookerId,
            @Positive(message = "ID вещи должен быть положительным числом")
            @RequestParam("itemId") Long itemId) {
        log.info("Получён запрос на добавление отзыва. Пользователь: {}, вещь: {}, текст отзыва: {}",
                bookerId, itemId, reqDto.getComment());
        CommentSendDto result = commentService.createComment(reqDto, bookerId, itemId);
        log.info("Выполнен запрос на добавление отзыва. Ответ клиенту: {}", result);
        return result;
    }

    /**
     * Обновление существующего отзыва.
     * <p>
     * Условия успешного выполнения:
     * - Отзыв существует в системе.
     * - Запрос отправляет автор отзыва (ID пользователя совпадает с автором комментария).
     */
    /*@PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommentSendDto updateComment(
            @Positive(message = "ID отзыва должен быть положительным числом")
            @PathVariable Long commentId,
            @Validated
            @RequestBody CommentReqDto reqDto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_USER_ID) Long bookerId) {
        log.info("Получён запрос на обновление отзыва ID: {} пользователем ID: {}. Новый текст: {}",
                commentId, bookerId, reqDto.getComment());
        CommentSendDto result = commentService.updateComment(commentId, reqDto, bookerId);
        log.info("Выполнен запрос на обновление отзыва. Ответ клиенту: {}", result);
        return result;
    }

    /**
     * Удаление отзыва из системы.
     * <p>
     * Условия успешного выполнения:
     * - Отзыв существует в системе.
     * - Запрос отправляет автор отзыва.
     */
   /* @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @Positive(message = "ID отзыва должен быть положительным числом")
            @PathVariable Long commentId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_USER_ID) Long bookerId) {
        log.info("Получён запрос на удаление отзыва ID: {} пользователем ID: {}", commentId, bookerId);
        commentService.deleteComment(commentId, bookerId);
        log.info("Отзыв ID: {} пользователя ID: {} успешно удалён", commentId, bookerId);
    }

    /**
     * Получение списка всех отзывов для конкретной вещи.
     * <p>
     * Особенности:
     * - Доступно всем пользователям без авторизации или проверки прав.
     * - Возвращает отзывы в порядке их создания (от старых к новым).
     * - Если отзывов нет, возвращает пустой список.
     */
  /*  @GetMapping("/item/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Collection<CommentSendDto> getCommentsByItem(
            @Positive(message = "ID вещи должен быть положительным числом")
            @PathVariable Long itemId) {
        log.info("Получён запрос на получение всех отзывов для вещи ID: {}", itemId);
        Collection<CommentSendDto> comments = commentService.getCommentsByItemId(itemId);
        log.info("Получено {} отзывов для вещи ID: {}", comments.size(), itemId);
        return comments;
    }
}
*/
