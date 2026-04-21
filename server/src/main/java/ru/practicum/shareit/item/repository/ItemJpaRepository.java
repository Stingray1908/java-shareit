package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.Item;

import java.util.List;

/**
 * Репозиторий для работы с сущностями Item (вещи) в системе ShareIt.
 * Предоставляет методы для поиска, фильтрации и получения расширенной информации о вещах,
 * включая данные о бронированиях и комментариях.
 *
 * <p>Основные сценарии использования:</p>
 * <ul>
 *   <li>Поиск вещей по запросу пользователя</li>
 *   <li>Поиск доступных вещей по текстовому запросу</li>
 *   <li>Получение информации о вещах владельца с данными о ближайших/последних бронированиях</li>
 *   <li>Загрузка вещей с прикреплёнными комментариями</li>
 * </ul>
 */
public interface ItemJpaRepository extends JpaRepository<Item, Long> {

    /**
     * Находит все вещи, связанные с определённым запросом (ItemRequest).
     * Используется для отображения предложений по запросу пользователя.
     *
     * @param id идентификатор запроса (ItemRequest)
     * @return список вещей, привязанных к запросу
     */
    List<Item> findByRequestId(Long id);

    /**
     * Выполняет поиск доступных вещей (available = true) по текстовой строке.
     * Поиск осуществляется по полям name и description с игнорированием регистра.
     * Результаты сортируются по имени (возрастание), затем по ID (убывание).
     *
     * @param text поисковый запрос (подстрока для поиска в name/description)
     * @return отсортированный список подходящих доступных вещей
     */
    @Query("SELECT i FROM Item i WHERE i.available = true "
            + "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :text, '%')) "
            + "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :text, '%'))) "
            + "ORDER BY i.name ASC, i.id DESC")
    List<Item> searchItems(@Param("text") String text);

    /**
     * Получает вещь по ID вместе с информацией о датах ближайшего и последнего бронирования.
     * Возвращает массив объектов, где:
     * <ul>
     *   <li>[0] — сущность Item</li>
     *   <li>[1] — LocalDateTime последнего бронирования (MAX(end) до текущего момента)</li>
     *   <li>[2] — LocalDateTime следующего бронирования (MIN(start) после текущего момента)</li>
     * </ul>
     * Учитываются только бронирования со статусами из параметра statuses.
     *
     * @param itemId   идентификатор вещи
     * @param statuses список статусов бронирований для учёта (например, 'APPROVED', 'COMPLETED')
     * @return список массивов объектов с информацией о вещи и датах бронирований (может быть пустым)
     */
    @Query("SELECT i, " +
            "(SELECT MAX(b1.end) FROM Booking b1 WHERE b1.item = i AND b1.end < CURRENT_TIMESTAMP AND b1.status IN :statuses) AS lastBooking, " +
            "(SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item = i AND b2.start > CURRENT_TIMESTAMP AND b2.status IN :statuses) AS nextBooking " +
            "FROM Item i " +
            "WHERE i.id = :itemId")
    List<Object[]> findItemWithBookingDatesById(@Param("itemId") Long itemId,
                                                @Param("statuses") List<String> statuses);

    /**
     * Получает все вещи владельца с информацией о датах ближайшего и последнего бронирования для каждой вещи.
     * Аналогичен findItemWithBookingDatesById, но для всех вещей владельца.
     * Результаты сортируются по ID вещи (возрастание).
     *
     * @param ownerId  идентификатор владельца вещей
     * @param statuses список статусов бронирований для учёта
     * @return список массивов объектов с информацией о вещах и датах их бронирований
     */
    @Query("SELECT i, " +
            "(SELECT MAX(b1.end) FROM Booking b1 WHERE b1.item = i AND b1.end < CURRENT_TIMESTAMP AND b1.status IN :statuses) AS lastBooking, " +
            "(SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item = i AND b2.start > CURRENT_TIMESTAMP AND b2.status IN :statuses) AS nextBooking " +
            "FROM Item i " +
            "WHERE i.owner.id = :ownerId " +
            "ORDER BY i.id")
    List<Object[]> findItemsWithBookingDatesByOwnerId(@Param("ownerId") Long ownerId,
                                                      @Param("statuses") List<String> statuses);

    /**
     * Находит все вещи владельца с загруженными комментариями (comments).
     * Использует LEFT JOIN FETCH для предотвращения проблемы N+1 при загрузке комментариев.
     * Результаты сортируются по ID вещи (возрастание).
     *
     * @param ownerId идентификатор владельца вещей
     * @return список вещей владельца с загруженными коллекциями комментариев
     */
    @Query("SELECT DISTINCT i FROM Item i " +
            "LEFT JOIN FETCH i.comments c " +
            "WHERE i.owner.id = :ownerId " +
            "ORDER BY i.id")
    List<Item> findItemsWithCommentsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT i, " +
            "(SELECT MAX(b1.end) FROM Booking b1 WHERE b1.item = i AND b1.end < CURRENT_TIMESTAMP AND b1.status IN :statuses) AS lastBooking, " +
            "(SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item = i AND b2.start > CURRENT_TIMESTAMP AND b2.status IN :statuses) AS nextBooking " +
            "FROM Item i " +
            "LEFT JOIN FETCH i.comments c " +
            "WHERE i.id = :itemId")
    List<Object[]> findItemWithBookingDatesAndCommentsById(
            @Param("itemId") Long itemId,
            @Param("statuses") List<String> statuses);

    @Query("SELECT i, " +
            "(SELECT MAX(b1.end) FROM Booking b1 WHERE b1.item = i AND b1.end < CURRENT_TIMESTAMP AND b1.status IN :statuses) AS lastBooking, " +
            "(SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item = i AND b2.start > CURRENT_TIMESTAMP AND b2.status IN :statuses) AS nextBooking " +
            "FROM Item i " +
            "LEFT JOIN FETCH i.comments c " +
            "WHERE i.owner.id = :ownerId " +
            "ORDER BY i.id")
    List<Object[]> findItemsWithBookingDatesAndCommentsByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("statuses") List<String> statuses);

    /**
     * Получает вещь по ID без комментариев, но с датами бронирований.
     * Используется для сценариев, где комментарии не требуются.
     */
    @Query("SELECT i, " +
            "(SELECT MAX(b1.end) FROM Booking b1 WHERE b1.item = i AND b1.end < CURRENT_TIMESTAMP AND b1.status IN :statuses) AS lastBooking, " +
            "(SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item = i AND b2.start > CURRENT_TIMESTAMP AND b2.status IN :statuses) AS nextBooking " +
            "FROM Item i " +
            "WHERE i.id = :itemId")
    List<Object[]> findItemWithBookingDatesOnlyById(
            @Param("itemId") Long itemId,
            @Param("statuses") List<String> statuses);

    /**
     * Получает вещи владельца без комментариев, но с датами бронирований.
     * Используется для списков вещей, где комментарии не нужны.
     */
    @Query("SELECT i, " +
            "(SELECT MAX(b1.end) FROM Booking b1 WHERE b1.item = i AND b1.end < CURRENT_TIMESTAMP AND b1.status IN :statuses) AS lastBooking, " +
            "(SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item = i AND b2.start > CURRENT_TIMESTAMP AND b2.status IN :statuses) AS nextBooking " +
            "FROM Item i " +
            "WHERE i.owner.id = :ownerId " +
            "ORDER BY i.id")
    List<Object[]> findItemsWithBookingDatesOnlyByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("statuses") List<String> statuses);
}

