package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.constants.HttpHeader;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.dto.ItemReqDTO;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<Object> create(@Valid @RequestBody ItemReqDTO itemReqDTO,
                                         @Positive(message = "ID пользователя должен быть положительным числом")
                                         @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long ownerId) {
        log.info("Ворота: получен запрос на добавление вещи. Владелец: {}, данные вещи: {}", ownerId, itemReqDTO);
        ResponseEntity<Object> result = itemClient.create(itemReqDTO, ownerId);
        log.info("Ворота: ответ от сервера получен для создания вещи");
        return result;
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(
            @Positive(message = "ID вещи должен быть положительным числом") @PathVariable Long itemId,
            @Valid @RequestBody CommentReqDto dto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        log.info("Ворота: получен запрос на добавление комментария к вещи ID: {} от пользователя ID: {}", itemId, userId);
        ResponseEntity<Object> result = itemClient.addComment(itemId, dto, userId);
        log.info("Ворота: ответ от сервера получен для добавления комментария");
        return result;
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> update(
            @Positive(message = "ID вещи должен быть положительным числом") @PathVariable Long itemId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId,
            @Valid @RequestBody ItemReqDTO itemReqDTO) {
        log.info("Ворота: получен запрос на обновление вещи ID: {} пользователем ID: {}. Данные для обновления: {}",
                itemId, userId, itemReqDTO);
        ResponseEntity<Object> result = itemClient.update(itemId, userId, itemReqDTO);
        log.info("Ворота: ответ от сервера получен для обновления вещи ID: {}", itemId);
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(
            @Positive(message = "ID вещи должен быть положительным числом") @PathVariable Long id) {
        log.info("Ворота: получен запрос на получение вещи по ID: {}", id);
        ResponseEntity<Object> result = itemClient.getById(id);
        log.info("Ворота: информация о вещи с ID: {} успешно получена", id);
        return result;
    }

    @GetMapping
    public ResponseEntity<Object> getOwnerItems(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) long ownerId) {
        log.info("Ворота: получен запрос на получение списка вещей пользователя ID: {}", ownerId);
        ResponseEntity<Object> result = itemClient.getOwnerItems(ownerId);
        log.info("Ворота: ответ от сервера получен для получения списка вещей пользователя ID: {}", ownerId);
        return result;
    }

    @GetMapping("request/{id}")
    public ResponseEntity<Object> getRequestItemsById(
            @Positive(message = "ID запроса должен быть положительным числом") @PathVariable Long id,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) long ownerId) {
        log.info("Ворота: получен запрос на получение вещей по запросу ID: {}, составителя ID: {}", id, ownerId);
        ResponseEntity<Object> result = itemClient.getRequestItemsById(id, ownerId);
        log.info("Ворота: вещи по запросу ID: {} успешно получены", id);
        return result;
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Object> deleteByItemAndOwnerIds(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) long userId,
            @Positive(message = "ID вещи должен быть положительным числом")
            @PathVariable(name = "itemId") long itemId) {
        log.info("Ворота: получен запрос на удаление вещи ID: {} пользователя ID: {}", itemId, userId);
        ResponseEntity<Object> result = itemClient.deleteByItemAndOwnerIds(userId, itemId);
        log.info("Ворота: вещь ID: {} пользователя ID: {} успешно удалена", itemId, userId);
        return result;
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam String text) {
        log.info("Ворота: получен запрос на поиск вещей по тексту: '{}'", text);
        ResponseEntity<Object> result = itemClient.search(text);
        log.info("Ворота: поиск по тексту '{}' дал результат", text);
        return result;
    }
}

