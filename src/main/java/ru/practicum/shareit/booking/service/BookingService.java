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

     * @throws NoSuchElementException   если пользователь или предмет не найдены
     * @throws IllegalArgumentException если:
     *                                  - владелец пытается забронировать свою вещь;
     *                                  - вещь недоступна (available = false);
     *                                  - поля бронирования некорректны (start >= end, start в прошлом);
     *                                  - есть активная бронь от этого пользователя на эту вещь;
     *                                  - новое бронирование пересекается по времени с существующими
     */
    BookingSendDto create(BookingReqDto dto, Long bookerId);


    /**
     * Подтверждает или отклоняет запрос на бронирование. Может быть выполнено только владельцем вещи.
     *
     * @param bookingId ID бронирования
     * @param approved true для статуса APPROVED, false для REJECTED
     * @param userId идентификатор владельца вещи
     * @return DTO с данными обновлённого бронирования
     * @throws NoSuchElementException если бронирование с указанным ID не найдено
     * @throws SecurityException если пользователь не является владельцем вещи
     * @throws IllegalArgumentException если статус бронирования не WAITING
     */
    BookingSendDto approveOrRejectBooking(Long bookingId, boolean approved, Long userId);

    /**
     * Получает данные бронирования по его идентификатору.
     *
     * @param bookingId идентификатор бронирования
     * @param userId    идентификатор пользователя, запрашивающего данные (booker или owner)
     * @return DTO с данными бронирования
     * @throws NoSuchElementException если бронирование не найдено или пользователь не имеет доступа
     * @throws SecurityException      если пользователь не является ни booker, ни owner предмета
     */
    BookingSendDto getByIdForBookerOrOwner(Long bookingId, Long userId);

    Collection<BookingSendDto> getBookingsByOwnerState(Long ownerId, String state);

    /**
     * Возвращает список бронирований пользователя с фильтрацией по состоянию.
     *
     * @param userId идентификатор пользователя
     * @param state строка состояния (ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED)
     * @return коллекция DTO с бронированиями
     * @throws NoSuchElementException если пользователь не найден
     * @throws IllegalArgumentException если state имеет недопустимое значение
     */
    Collection<BookingSendDto> getBookingsByState(Long userId, String state);



    Booking findByIdOrThrowInternal(Long id);

}
