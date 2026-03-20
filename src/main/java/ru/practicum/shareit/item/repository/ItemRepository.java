package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.Item;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Репозиторий для работы с предметами (вещами) в системе.
 */
public interface ItemRepository {

    /**
     * Возвращает список предметов, принадлежащих указанному владельцу.
     *
     * @param userId идентификатор пользователя — владельца предметов
     * @return список предметов пользователя, отсортированный по ID
     */
    List<Item> findByUserId(long userId);

    /**
     * Получает коллекцию предметов, связанных с указанным запросом.
     *
     * @param requestId идентификатор запроса, к которому привязаны предметы
     * @return коллекция предметов, соответствующих запросу
     */
    Collection<Item> getItemsByRequestIds(Long requestId);

    /**
     * Находит предмет по его идентификатору.
     *
     * @param itemId идентификатор предмета
     * @return Optional с сущностью предмета, если найден, или пустой Optional
     */
    Optional<Item> getById(long itemId);

    /**
     * Сохраняет новый предмет в хранилище.
     *
     * @param item сущность предмета для сохранения
     * @return сохранённая сущность предмета с присвоенным ID
     */
    Item create(Item item);

    /**
     * Обновляет данные существующего предмета.
     *
     * @param itemId  идентификатор предмета, который нужно обновить
     * @param ownerId идентификатор владельца предмета (для проверки прав доступа)
     * @param item    сущность с новыми данными предмета
     * @return обновлённая сущность предмета
     * @throws NoSuchElementException если предмет с указанным ID не найден
     * @throws NoSuchElementException если указанный владелец не является владельцем предмета
     */
    Item update(Long itemId, Long ownerId, Item item);

    /**
     * Выполняет поиск доступных предметов по текстовому запросу.
     * Ищет совпадения в полях name и description (без учёта регистра).
     *
     * @param text поисковый запрос (подстрока для поиска)
     * @return список доступных предметов, чьи name или description содержат поисковую подстроку
     * Возвращает пустой список, если текст поиска пустой или пробелов
     */
    List<Item> search(String text);

    /**
     * Удаляет предмет из хранилища, если он принадлежит указанному владельцу.
     *
     * @param itemId  идентификатор предмета для удаления
     * @param ownerId идентификатор владельца предмета
     * @return true, если предмет успешно удалён; false, если предмет не найден или не принадлежит владельцу
     * @throws NoSuchElementException если предмет с указанным ID не найден (в процессе проверки)
     */
    boolean deleteByItemAndOwnerIds(long itemId, long ownerId);
}
