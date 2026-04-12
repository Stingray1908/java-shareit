/**
 * Репозиторий для работы с сущностями Booking (бронирования) в системе ShareIt.
 * Предоставляет методы для CRUD‑операций и специализированных запросов по управлению
 * бронированиями с учётом статусов, временных периодов и ролей пользователей
 * (booker — тот, кто бронирует; owner — владелец вещи).
 *
 * <p>Основные сценарии использования:</p>
 * <ul>
 *   <li>Поиск бронирований по ID с загрузкой связанных данных (пользователь, вещь)</li>
 *   <li>Проверка доступности вещи на заданный период (отсутствие пересекающихся бронирований)</li>
 *   <li>Просмотр всех, текущих, прошлых и будущих бронирований пользователя‑booker</li>
 *   <li>Фильтрация бронирований по статусам</li>
 *   <li>Получение информации о бронированиях вещей владельца (все, текущие, прошлые, будущие)</li>
 *   <li>Валидация возможности оставить комментарий (проверка факта аренды вещи пользователем)</li>
 * </ul>
 *
 * <p>Группировка методов по функциональному назначению:</p>
 * <ol>
 *   <li><b>Базовые операции</b> — стандартные CRUD‑методы и загрузка с ассоциациями</li>
 *   <li><b>Проверка конфликтов бронирований</b> — валидация доступности вещи</li>
 *   <li><b>Запросы по пользователю‑booker</b> — поиск бронирований того, кто бронирует</li>
 *   <li><b>Запросы по владельцу вещи (owner)</b> — поиск бронирований для вещей владельца</li>
 * </ol>
 */
package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.common.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingJpaRepository extends JpaRepository<Booking, Long> {

    // === БАЗОВЫЕ ОПЕРАЦИИ ===

    /**
     * Найти бронирование по ID.
     *
     * @param id идентификатор бронирования
     * @return Optional<Booking> — бронирование, если найдено, иначе пустой Optional
     */
    Optional<Booking> findById(Long id);

    /**
     * Найти бронирование по ID с загрузкой связанных сущностей (booker и item) через JOIN FETCH
     * для предотвращения проблемы N+1.
     *
     * @param id идентификатор бронирования
     * @return Optional<Booking> с загруженными ассоциациями
     */
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.booker " +
            "JOIN FETCH b.item " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithAssociations(@Param("id") Long id);

    // === ПРОВЕРКА КОНФЛИКТОВ БРОНИРОВАНИЙ ===

    /**
     * Проверяет, есть ли пересекающиеся бронирования для вещи в заданном временном интервале.
     * Учитывает только статусы APPROVED и WAITING.
     *
     * @param itemId ID вещи
     * @param start  начало периода проверки
     * @param end    конец периода проверки
     * @return true, если есть пересекающиеся бронирования; false — если нет
     */
    @Query("SELECT COUNT(b) > 0 " +
            "FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status IN (APPROVED, WAITING) " +
            "AND b.start < :end " +
            "AND b.end > :start")
    boolean checkOverLapBookings(
            @Param("itemId") Long itemId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // === ЗАПРОСЫ ПО ПОЛЬЗОВАТЕЛЮ‑BOOKER (тот, кто бронирует) ===

    /**
     * Найти все бронирования пользователя‑booker.
     *
     * @param bookerId идентификатор пользователя, который бронирует
     * @return список всех бронирований пользователя
     */
    List<Booking> findAllByBookerId(Long bookerId);

    /**
     * Найти текущие бронирования пользователя (пересекающиеся с текущим моментом).
     *
     * @param bookerId        идентификатор пользователя
     * @param currentDateTime текущая дата и время
     * @return список текущих бронирований (start <= currentDateTime < end)
     */
    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.booker.id = :bookerId " +
            "AND b.start <= :currentDateTime " +
            "AND b.end > :currentDateTime")
    List<Booking> findCurrentBookings(
            @Param("bookerId") Long bookerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти завершённые бронирования пользователя.
     *
     * @param bookerId        идентификатор пользователя
     * @param currentDateTime текущая дата и время
     * @return список завершённых бронирований (end <= currentDateTime)
     */
    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.booker.id = :bookerId " +
            "AND b.end <= :currentDateTime")
    List<Booking> findPastBookings(
            @Param("bookerId") Long bookerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти будущие бронирования пользователя.
     *
     * @param bookerId        идентификатор пользователя
     * @param currentDateTime текущая дата и время
     * @return список будущих бронирований (start > currentDateTime)
     */
    @Query("SELECT b " +
            "FROM Booking b " +
            "WHERE b.booker.id = :bookerId " +
            "AND b.start > :currentDateTime")
    List<Booking> findFutureBookings(
            @Param("bookerId") Long bookerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти бронирования пользователя по статусу.
     *
     * @param bookerId идентификатор пользователя
     * @param status   статус бронирования (BookingStatus)
     * @return список бронирований с указанным статусом
     */
    List<Booking> findByBookerIdAndStatus(Long bookerId, BookingStatus status);

    // === ЗАПРОСЫ ПО ВЛАДЕЛЬЦУ ВЕЩИ (OWNER) ===

    /**
     * Найти все бронирования для вещей пользователя‑владельца.
     *
     * @param ownerId идентификатор владельца вещей
     * @return список всех бронирований вещей владельца
     */
    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId")
    List<Booking> findAllByItemOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Найти текущие бронирования для вещей пользователя‑владельца.
     *
     * @param ownerId         идентификатор владельца
     * @param currentDateTime текущая дата и время
     * @return список текущих бронирований вещей владельца (start <= currentDateTime < end)
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.start <= :currentDateTime " +
            "AND b.end > :currentDateTime")
    List<Booking> findCurrentBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти завершённые бронирования для вещей пользователя‑владельца.
     *
     * @param ownerId         идентификатор владельца
     * @param currentDateTime текущая дата и время
     * @return список завершённых бронирований вещей владельца (end <= currentDateTime)
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.end <= :currentDateTime")
    List<Booking> findPastBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти будущие бронирования для вещей пользователя‑владельца.
     *
     * @param ownerId         идентификатор владельца
     * @param currentDateTime текущая дата и время
     * @return список будущих бронирований вещей владельца (start > currentDateTime)
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.start > :currentDateTime")
    List<Booking> findFutureBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти бронирования для вещей владельца по статусу (только с завершёнными датами).
     *
     * @param ownerId идентификатор владельца
     * @param status  статус бронирования (BookingStatus)
     * @return список бронирований вещей владельца с указанным статусом и завершёнными датами
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.status = :status " +
            "AND b.end < CURRENT_TIMESTAMP")
    List<Booking> findByItemOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") BookingStatus status
    );

    /**
     * Проверяет, брал ли пользователь вещь в аренду ранее (используется для валидации
     * перед оставлением комментария). Учитывает бронирования с указанными статусами,
     * которые завершились до текущего момента.
     *
     * @param bookerId        идентификатор пользователя, который бронирует
     * @param itemId          идентификатор вещи
     * @param statuses        список статусов бронирований для учёта
     * @param currentDateTime текущая дата и время
     * @return true, если пользователь ранее брал вещь в аренду; false — если не брал
     */
    @Query("SELECT COUNT(b) > 0 " +
            "FROM Booking b " +
            "WHERE b.booker.id = :bookerId " +
            "  AND b.item.id = :itemId " +
            "  AND b.status IN :statuses " +
            "  AND b.end < :currentDateTime")
    boolean existsPastBooking(
            @Param("bookerId") Long bookerId,
            @Param("itemId") Long itemId,
            @Param("statuses") List<String> statuses,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );
}

