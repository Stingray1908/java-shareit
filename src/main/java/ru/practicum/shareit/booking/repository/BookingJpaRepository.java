package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.common.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с бронированиями.
 * Группировка методов по функциональному назначению:
 * 1. Базовые операции
 * 2. Проверка конфликтов бронирований
 * 3. Запросы по пользователю‑booker
 * 4. Запросы по владельцу вещи (owner)
 */
public interface BookingJpaRepository extends JpaRepository<Booking, Long> {

    // === БАЗОВЫЕ ОПЕРАЦИИ ===

    /**
     * Найти бронирование по ID.
     */
    Optional<Booking> findById(Long id);

    // === ПРОВЕРКА КОНФЛИКТОВ БРОНИРОВАНИЙ ===

    /**
     * Проверяет, есть ли пересекающиеся бронирования для вещи.
     * Учитывает только статусы APPROVED и WAITING.
     *
     * @param itemId ID вещи
     * @param start  Начало периода проверки
     * @param end    Конец периода проверки
     * @return true, если есть пересекающиеся бронирования
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
     */
    List<Booking> findAllByBookerId(Long bookerId);

    /**
     * Найти текущие бронирования пользователя (пересекающиеся с текущим моментом).
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
     */
    List<Booking> findByBookerIdAndStatus(Long bookerId, BookingStatus status);

    // === ЗАПРОСЫ ПО ВЛАДЕЛЬЦУ ВЕЩИ (OWNER) ===

    /**
     * Найти все бронирования для вещей пользователя‑владельца.
     */
    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId")
    List<Booking> findAllByItemOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Найти текущие бронирования для вещей пользователя‑владельца.
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
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.start > :currentDateTime")
    List<Booking> findFutureBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentDateTime") LocalDateTime currentDateTime
    );

    /**
     * Найти бронирования для вещей владельца по статусу.
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.status = :status")
    List<Booking> findByItemOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") BookingStatus status
    );
}
