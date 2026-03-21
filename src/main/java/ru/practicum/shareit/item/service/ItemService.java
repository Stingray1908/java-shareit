package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Сервис для управления предметами (вещами) в системе.
 */
public interface ItemService {

    /**
     * Добавляет новую вещь в систему от имени указанного владельца.
     *
     * @param ownerId    идентификатор пользователя — владельца вещи
     * @param itemReqDTO DTO с данными для создания вещи
     * @return DTO с данными добавленной вещи
     * @throws NoSuchElementException   если пользователь с указанным ID не найден
     * @throws IllegalArgumentException если описание превышает допустимую длину (100 символов)
     */
    ItemSendDTO create(Long ownerId, ItemReqDTO itemReqDTO);

    /**
     * Обновляет данные существующей вещи.
     *
     * @param itemId     идентификатор вещи, которую нужно обновить
     * @param ownerId    идентификатор владельца вещи (для проверки прав доступа)
     * @param itemReqDTO DTO с обновлёнными данными вещи
     * @return DTO с данными обновлённой вещи
     * @throws NoSuchElementException   если вещь с указанным ID не найдена
     * @throws IllegalArgumentException если:
     *                                  - при обновлении не указано ни одного поля;
     *                                  - описание превышает допустимую длину (100 символов)
     */
    ItemSendDTO update(Long itemId, Long ownerId, ItemReqDTO itemReqDTO);

    /**
     * Получает DTO вещи по её идентификатору для внешнего использования (клиента).
     *
     * @param id идентификатор вещи
     * @return DTO с данными вещи
     * @throws NoSuchElementException если вещь с указанным ID не найдена
     */
    ItemSendDTO getById(Long id);

    /**
     * Получает сущность вещи по её идентификатору для внутреннего использования в системе.
     *
     * @param id идентификатор вещи
     * @return сущность вещи
     * @throws NoSuchElementException если вещь с указанным ID не найдена
     */
    Item getByIdInternal(Long id);

    /**
     * Возвращает список DTO вещей, принадлежащих указанному пользователю.
     *
     * @param userId идентификатор пользователя — владельца вещей
     * @return список DTO с вещами пользователя
     * @throws NoSuchElementException если пользователь с указанным ID не зарегистрирован в системе
     */
    List<ItemSendDTO> getOwnerItems(long userId);

    /**
     * Получает коллекцию DTO вещей, связанных с указанным запросом и принадлежащих владельцу запроса.
     *
     * @param requestId      идентификатор запроса, к которому привязаны вещи
     * @param requestOwnerId идентификатор владельца запроса (для проверки прав доступа)
     * @return коллекция DTO с вещами, соответствующими запросу
     * @throws NoSuchElementException если запрос с указанным ID не найден
     * @throws SecurityException      если пользователь не является владельцем запроса
     */
    Collection<ItemSendDTO> getItemsByRequestOwnerAndRequestIds(Long requestId, Long requestOwnerId);

    /**
     * Удаляет вещь из системы, если она принадлежит указанному пользователю.
     *
     * @param userId идентификатор пользователя — владельца вещи
     * @param itemId идентификатор вещи для удаления
     * @throws NoSuchElementException если пользователь или вещь не найдены
     * @throws SecurityException      если указанный пользователь не является владельцем вещи
     */
    void deleteByItemAndOwnerIds(long userId, long itemId);

    /**
     * Осуществляет поиск доступных вещей по подстроке в названии или описании (без учёта регистра).
     * Возвращает только вещи с флагом available = true.
     *
     * @param text поисковая строка (подстрока для поиска)
     * @return список DTO найденных вещей, чьи name или description содержат поисковую подстроку
     * Возвращает пустой список, если текст поиска пустой или состоит только из пробелов
     */
    List<ItemSendDTO> search(String text);
}
