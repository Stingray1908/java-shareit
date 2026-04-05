/**
 * Сервис для управления бронированиями предметов в системе ShareIt.
 * Предоставляет методы для создания, подтверждения, получения и фильтрации бронирований
 * с учётом бизнес‑правил и прав доступа.
 *
 * <p>Основные сценарии использования:</p>
 * <ul>
 *   <li>Создание нового бронирования с валидацией доступности вещи и корректности дат</li>
 *   <li>Подтверждение/отклонение бронирования владельцем вещи</li>
 *   <li>Получение информации о бронировании для booker или owner</li>
 *   <li>Просмотр списка бронирований пользователя с фильтрацией по состоянию</li>
 *   <li>Просмотр бронирований вещей владельца с фильтрацией по состоянию</li>
 * </ul>
 *
 * <p>Ключевые бизнес‑правила:</p>
 * <ul>
 *   <li>Владелец не может бронировать свои вещи</li>
 *   <li>Вещь должна быть доступна (available = true)</li>
 *   <li>Даты бронирования должны быть корректны (start < end, start в будущем)</li>
 *   <li>Не допускается пересечение бронирований по времени</li>
 *   <li>Подтверждение возможно только для статуса WAITING</li>
 *   <li>Доступ к данным бронирования имеют только booker и owner</li>
 * </ul>
 */
package ru.practicum.shareit.booking.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;

import java.util.Collection;
import java.util.NoSuchElementException;

@Service
public interface BookingService {

    /**
     * Создаёт новое бронирование предмета.
     *
     * @param dto     DTO с данными бронирования (itemId, start, end)
     * @param bookerId идентификатор пользователя, который бронирует
     * @return DTO с данными созданного бронирования (включая ID и статус WAITING)
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
     * @param approved  true для статуса APPROVED, false для REJECTED
     * @param userId    идентификатор владельца вещи
     * @return DTO с данными обновлённого бронирования
     * @throws NoSuchElementException   если бронирование с указанным ID не найдено
     * @throws SecurityException        если пользователь не является владельцем вещи
     * @throws IllegalArgumentException если статус бронирования не WAITING
     */
    BookingSendDto approveOrRejectBooking(Long bookingId, boolean approved, Long userId);

    /**
     * Получает данные бронирования по его идентификатору.
     * Доступ имеют только booker (тот, кто бронировал) и owner (владелец вещи).
     *
     * @param bookingId идентификатор бронирования
     * @param userId    идентификатор пользователя, запрашивающего данные (booker или owner)
     * @return DTO с данными бронирования
     * @throws NoSuchElementException если бронирование не найдено или пользователь не имеет доступа
     * @throws SecurityException      если пользователь не является ни booker, ни owner предмета
     */
    BookingSendDto getByIdForBookerOrOwner(Long bookingId, Long userId);

    /**
     * Возвращает список бронирований владельца вещи с фильтрацией по состоянию.
     *
     * @param ownerId идентификатор владельца вещей
     * @param state   строка состояния (ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED)
     * @return коллекция DTO с бронированиями вещей владельца
     * @throws NoSuchElementException   если владелец не найден
     * @throws IllegalArgumentException если state имеет недопустимое значение
     */
    Collection<BookingSendDto> getBookingsByOwnerState(Long ownerId, String state);

    /**
     * Возвращает список бронирований пользователя с фильтрацией по состоянию.
     *
     * @param userId идентификатор пользователя
     * @param state  строка состояния (ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED)
     * @return коллекция DTO с бронированиями
     * @throws NoSuchElementException   если пользователь не найден
     * @throws IllegalArgumentException если state имеет недопустимое значение
     */
    Collection<BookingSendDto> getBookingsByState(Long userId, String state);

    /**
     * Вспомогательный метод для внутреннего использования — получает бронирование по ID
     * или выбрасывает исключение, если не найдено.
     *
     * @param id идентификатор бронирования
     * @return сущность Booking
     * @throws NoSuchElementException если бронирование с указанным ID не существует
     */
    Booking findByIdOrThrowInternal(Long id);
}
