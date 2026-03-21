package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.constants.HttpHeader;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.service.ItemService;

import java.util.Collection;
import java.util.List;

/**
 * REST‑контроллер для управления вещами (items) в системе.
 * Предоставляет полный набор API‑методов для выполнения операций CRUD (создание, чтение, обновление, удаление)
 * над сущностями вещей, а также дополнительные функции поиска и фильтрации.
 * <p>
 * Основные правила бизнес‑логики:
 * - Создание вещи доступно только авторизованному владельцу (ID пользователя передаётся в заголовке X‑Sharer‑User‑Id).
 * - Обновление вещи (частичное, PATCH) может выполнять только её владелец.
 * - Удаление вещи доступно исключительно её владельцу.
 * - Получение информации о конкретной вещи доступно любому пользователю.
 * - Просмотр списка вещей пользователя доступен только самому пользователю.
 * - Поиск вещей по подстроке в названии или описании доступен всем пользователям.
 * - При создании и обновлении вещи выполняется валидация данных:
 * - описание не может превышать 100 символов;
 * - при обновлении хотя бы одно поле должно быть заполнено.
 * - Все операции, требующие ID вещи или пользователя, проверяют их существование в системе.
 * Если сущность не найдена, выбрасывается NoSuchElementException (404).
 * - ID вещи и пользователя должны быть положительными числами во всех запросах, где они используются.
 * - При попытке выполнить операцию над чужой вещью выбрасывается SecurityException (403).
 * - Статус ответа при успешном удалении — NO_CONTENT (204).
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * Создаёт новую вещь в системе.
     * Доступно только авторизованному пользователю (ID передаётся в заголовке X‑Sharer‑User‑Id).
     * Выполняет валидацию описания (не более 100 символов).
     * Возвращает ItemSendDTO с присвоенным ID вещи.
     * <p>
     * Особенности работы с запросами:
     * - Если вещь создаётся с указанием requestId, система попытается изменить статус
     * соответствующего запроса с PENDING на RESPONDED.
     * - Вещь будет создана в любом случае, независимо от текущего статуса запроса
     * (даже если запрос уже обработан или имеет другой статус).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemSendDTO create(@Valid
                              @RequestBody ItemReqDTO itemReqDTO,
                              @Positive(message = "ID пользователя должен быть положительным числом")
                              @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long ownerId) {
        log.info("Получён запрос на добавление вещи. Владелец: {}, данные вещи: {}", ownerId, itemReqDTO);
        ItemSendDTO result = itemService.create(ownerId, itemReqDTO);
        log.info("Выполнен запрос на добавление вещи. Ответ клиенту: {}", result);
        return result;
    }

    /**
     * Частично обновляет информацию о вещи (PATCH): название, описание, статус доступности.
     * Доступно только владельцу вещи.
     * ID вещи передаётся в пути запроса, ID владельца — в заголовке X‑Sharer‑User‑Id.
     * Проверяет существование вещи и принадлежность её указанному владельцу.
     * При отсутствии обновляемых полей выбрасывает IllegalArgumentException (400).
     */
    @PatchMapping("/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemSendDTO update(@Positive(message = "ID вещи должен быть положительным числом")
                              @PathVariable Long itemId,
                              @Positive(message = "ID пользователя должен быть положительным числом")
                              @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId,
                              @RequestBody ItemReqDTO itemReqDTO) {
        log.info("Получён запрос на обновление вещи ID: {} пользователем ID: {}. Данные для обновления: {}",
                itemId, userId, itemReqDTO);
        ItemSendDTO result = itemService.update(itemId, userId, itemReqDTO);
        log.info("Выполнен запрос на обновление вещи. Ответ клиенту: {}", result);
        return result;
    }

    /**
     * Возвращает информацию о конкретной вещи по её ID.
     * Доступно любому пользователю.
     * Если вещь не найдена, выбрасывает NoSuchElementException (404).
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ItemSendDTO getById(@Positive(message = "ID вещи должен быть положительным числом")
                               @PathVariable Long id) {
        log.info("Получён запрос на получение вещи по ID: {}", id);
        ItemSendDTO item = itemService.getById(id);
        log.info("Вещь по ID: {} успешно получена. Ответ клиенту: {}", id, item);
        return item;
    }

    /**
     * Возвращает список всех вещей, принадлежащих конкретному пользователю.
     * Доступно только самому пользователю (ID передаётся в заголовке X‑Sharer‑User‑Id).
     * Если пользователь не найден, выбрасывает NoSuchElementException (404).
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<ItemSendDTO> getOwnerItems(@Positive(message = "ID пользователя должен быть положительным числом")
                                                 @RequestHeader(HttpHeader.X_SHARER_USER_ID) long ownerId) {
        log.info("Получён запрос на получение списка вещей пользователя ID: {}", ownerId);
        List<ItemSendDTO> items = itemService.getOwnerItems(ownerId);
        log.info("Получено {} вещей для пользователя ID: {}", items.size(), ownerId);
        return items;
    }

    /**
     * Возвращает список вещей, связанных с конкретным запросом по его ID.
     * Доступно только автору запроса.
     * Проверяет принадлежность запроса указанному пользователю.
     * Если запрос не найден или пользователь не является его автором, выбрасывает SecurityException (403).
     */
    @GetMapping("request/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ItemSendDTO> getRequestItemsById(@Positive(message = "ID запроса должен быть положительным числом")
                                                       @PathVariable Long id,
                                                       @Positive(message = "ID пользователя должен быть положительным числом")
                                                       @RequestHeader(HttpHeader.X_SHARER_USER_ID) long ownerId) {
        log.info("Получён запрос на получение вещей по запросу ID: {}, составителя ID: {}", id, ownerId);
        Collection<ItemSendDTO> items = itemService.getItemsByRequestOwnerAndRequestIds(id, ownerId);
        log.info("Вещи по запросу ID: {} успешно получены. Количество вещей: {}", id, items.size());
        return items;
    }

    /**
     * Удаляет вещь из системы.
     * Доступно только владельцу вещи.
     * ID вещи передаётся в пути запроса, ID владельца — в заголовке X‑Sharer‑User‑Id.
     * Если вещь не найдена или не принадлежит пользователю, выбрасывает SecurityException (403).
     * Статус ответа при успехе — NO_CONTENT (204).
     */
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByItemAndOwnerIds(@Positive(message = "ID пользователя должен быть положительным числом")
                                        @RequestHeader(HttpHeader.X_SHARER_USER_ID) long userId,
                                        @Positive(message = "ID вещи должен быть положительным числом")
                                        @PathVariable(name = "itemId") long itemId) {
        log.info("Получён запрос на удаление вещи ID: {} пользователя ID: {}", itemId, userId);
        itemService.deleteByItemAndOwnerIds(userId, itemId);
        log.info("Вещь ID: {} пользователя ID: {} успешно удалена", itemId, userId);
    }

    /**
     * Ищет вещи по подстроке в названии или описании.
     * Доступен всем пользователям.
     * Поиск нечувствителен к регистру.
     * При пустой или null‑строке возвращает пустой список.
     */
    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ItemSendDTO> search(@RequestParam String text) {
        log.info("Получён запрос на поиск вещей по тексту: '{}'", text);
        Collection<ItemSendDTO> results = itemService.search(text);
        log.info("Поиск по тексту '{}' дал {} результатов", text, results.size());
        return results;
    }
}
