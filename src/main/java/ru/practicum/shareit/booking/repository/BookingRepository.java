package ru.practicum.shareit.booking.repository;

import com.sun.jdi.InternalException;
import ru.practicum.shareit.booking.Booking;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;

/**
 * Репозиторий для работы с бронированиями предметов в системе.
 */
public interface BookingRepository {

    /**
     * Сохраняет новое бронирование в хранилище.
     *
     * @param booking сущность бронирования для сохранения
     * @return сохранённая сущность бронирования с присвоенным ID
     * @throws InternalException если произошла ошибка при сохранении
     */
    Booking addBooking(Booking booking);

    /**
     * Обновляет статус существующего бронирования.
     *
     * @param booking сущность бронирования с обновлённым статусом
     * @return обновлённая сущность бронирования
     * @throws InternalException если бронирование с указанным ID не найдено
     */
    Booking patchBooking(Booking booking);

    /**
     * Удаляет бронирование из хранилища.
     *
     * @param booking сущность бронирования для удаления
     */
    void deleteBooking(Booking booking);

    /**
     * Находит бронирование по его идентификатору.
     *
     * @param bookingId идентификатор бронирования
     * @return Optional с сущностью бронирования, если найдено, или пустой Optional
     */
    Optional<Booking> findBooking(Long bookingId);

    /**
     * Возвращает упорядоченный набор бронирований для указанного предмета.
     * Бронирования отсортированы по времени начала (start).
     *
     * @param itemId идентификатор предмета
     * @return NavigableSet с бронированиями предмета (пустой, если бронирований нет)
     */
    NavigableSet<Booking> getItemBookings(Long itemId);

    /**
     * Получает коллекцию всех бронирований, созданных указанным пользователем.
     *
     * @param bookerId идентификатор пользователя — booker
     * @return коллекция сущностей бронирований пользователя
     */
    Collection<Booking> getCreatedBookings(Long bookerId);
}
