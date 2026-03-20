package ru.practicum.shareit.booking.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;


import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Сервис для управления бронированиями предметов в системе.
 */
@Service
public interface BookingService {

    /**
     * Создаёт новое бронирование предмета.
     *
     * @param reqDto DTO с данными бронирования (itemId, bookerId, start, end)
     * @return DTO с данными созданного бронирования со статусом WAITING
     * @throws NoSuchElementException   если пользователь или предмет не найдены
     * @throws IllegalArgumentException если:
     *                                  - владелец пытается забронировать свою вещь;
     *                                  - вещь недоступна (available = false);
     *                                  - поля бронирования некорректны (start >= end, start в прошлом);
     *                                  - есть активная бронь от этого пользователя на эту вещь;
     *                                  - новое бронирование пересекается по времени с существующими
     */
    BookingSendDto addBooking(BookingReqDto reqDto);

    /**
     * Обновляет статус существующего бронирования.
     *
     * @param reqDto DTO с ID бронирования и новым статусом
     * @param userId идентификатор пользователя, запрашивающего изменение (booker или owner)
     * @return DTO с данными обновлённого бронирования
     * @throws NoSuchElementException   если бронирование с указанным ID не найдено
     * @throws IllegalArgumentException если:
     *                                  - бронирование уже завершено (REJECTED, CANCELED, COMPLETED);
     *                                  - booker пытается установить статус, отличный от CANCELED;
     *                                  - owner нарушает правила изменения статуса
     * @throws SecurityException        если пользователь не имеет доступа к бронированию
     */
    BookingSendDto patchBooking(BookingReqDto reqDto, Long userId);

    /**
     * Удаляет бронирование.
     *
     * @param bookingId идентификатор бронирования для удаления
     * @param bookerId  идентификатор пользователя — создателя бронирования
     * @throws NoSuchElementException   если бронирование не найдено
     * @throws IllegalArgumentException если пользователь не является booker данного бронирования
     */
    void deleteBooking(Long bookingId, Long bookerId);

    /**
     * Получает данные бронирования по его идентификатору.
     *
     * @param bookingId идентификатор бронирования
     * @param userId    идентификатор пользователя, запрашивающего данные (booker или owner)
     * @return DTO с данными бронирования
     * @throws NoSuchElementException если бронирование не найдено или пользователь не имеет доступа
     * @throws SecurityException      если пользователь не является ни booker, ни owner предмета
     */
    BookingSendDto getBooking(Long bookingId, Long userId);

    /**
     * Возвращает список всех бронирований, созданных указанным пользователем.
     *
     * @param userId идентификатор пользователя — booker
     * @return коллекция DTO с бронированиями пользователя
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    Collection<BookingSendDto> getCreatedBookings(Long userId);

    /**
     * Получает коллекцию сущностей бронирований для указанного предмета (внутренний метод).
     * Используется для проверки пересечений бронирований и валидации.
     *
     * @param itemId идентификатор предмета
     * @return коллекция сущностей бронирований предмета
     * @throws NoSuchElementException если предмет с указанным ID не найден
     */
    Collection<Booking> getItemBookingsInternal(Long itemId);

    /**
     * Получает коллекцию DTO бронирований для указанного предмета (внешний метод).
     *
     * @param itemId идентификатор предмета
     * @return коллекция DTO с бронированиями предмета
     */
    Collection<BookingSendDto> getItemBookingsExternal(Long itemId);
}
