package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.HttpHeader;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.service.ItemService;

import java.util.Collection;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemSendDTO create(@RequestBody ItemReqDTO itemReqDTO,
                              @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long ownerId) {
        log.info("Сервер: получен запрос на добавление вещи. Владелец: {}, данные вещи: {}", ownerId, itemReqDTO);
        ItemSendDTO result = itemService.create(ownerId, itemReqDTO);
        log.info("Сервер: выполнен запрос на добавление вещи. Ответ клиенту: {}", result);
        return result;
    }

    @PostMapping("/{itemId}/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentSendDto addComment(@PathVariable Long itemId,
                                     @RequestBody CommentReqDto dto,
                                     @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        log.info("Сервер: получен запрос на добавление комментария к вещи ID: {} от пользователя ID: {}", itemId, userId);
        log.info(String.valueOf(dto));
        CommentSendDto result = itemService.addComment(userId, itemId, dto);
        log.info("Сервер: выполнен запрос на добавление комментария. Ответ клиенту: {}", result);
        return result;
    }

    @PatchMapping("/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemSendDTO update(@PathVariable Long itemId,
                              @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId,
                              @RequestBody ItemReqDTO itemReqDTO) {
        log.info("Сервер: получен запрос на обновление вещи ID: {} пользователем ID: {}. Данные для обновления: {}",
                itemId, userId, itemReqDTO);
        ItemSendDTO result = itemService.update(itemId, userId, itemReqDTO);
        log.info("Сервер: выполнен запрос на обновление вещи. Ответ клиенту: {}", result);
        return result;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ItemSendDTO getById(@PathVariable Long id) {
        log.info("Сервер: получен запрос на получение вещи по ID: {}", id);
        ItemSendDTO item = itemService.getById(id);
        log.info("Сервер: вещь по ID: {} успешно получена. Ответ клиенту: {}", id, item);
        return item;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<ItemSendDTO> getOwnerItems(@RequestHeader(HttpHeader.X_SHARER_USER_ID) long ownerId) {
        log.info("Сервер: получен запрос на получение списка вещей пользователя ID: {}", ownerId);
        List<ItemSendDTO> items = itemService.getOwnerItems(ownerId);
        log.info("Сервер: получено {} вещей для пользователя ID: {}", items.size(), ownerId);
        return items;
    }

    @GetMapping("request/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ItemSendDTO> getRequestItemsById(@PathVariable Long id,
                                                       @RequestHeader(HttpHeader.X_SHARER_USER_ID) long ownerId) {
        log.info("Сервер: получен запрос на получение вещей по запросу ID: {}, составителя ID: {}", id, ownerId);
        Collection<ItemSendDTO> items = itemService.findItemsByRequestIdForRequester(id, ownerId);
        log.info("Сервер: вещи по запросу ID: {} успешно получены. Количество вещей: {}", id, items.size());
        return items;
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByItemAndOwnerIds(@RequestHeader(HttpHeader.X_SHARER_USER_ID) long userId,
                                        @PathVariable(name = "itemId") long itemId) {
        log.info("Сервер: получен запрос на удаление вещи ID: {} пользователя ID: {}", itemId, userId);
        itemService.deleteByIdForOwner(userId, itemId);
        log.info("Сервер: вещь ID: {} пользователя ID: {} успешно удалена", itemId, userId);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ItemSendDTO> search(@RequestParam String text) {
        log.info("Сервер: получен запрос на поиск вещей по тексту: '{}'", text);
        Collection<ItemSendDTO> results = itemService.search(text);
        log.info("Сервер: поиск по тексту '{}' дал {} результатов", text, results.size());
        return results;
    }
}
