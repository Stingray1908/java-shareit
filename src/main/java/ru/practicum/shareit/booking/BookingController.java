package ru.practicum.shareit.booking;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.constants.HttpHeader;

import java.util.Collection;

@Slf4j
@Validated
@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService bookingService) {
        this.service = bookingService;
    }

    /**
     * Создаёт новое бронирование вещи.
     * Устанавливает статус WAITING. Проверяет доступность вещи, корректность дат и права пользователя.
     * ID пользователя передаётся в заголовке X-Booker-User-Id.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingSendDto addBooking(
            @RequestBody @Validated BookingReqDto bookingReqDto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long bookerId) {
        log.info("Получен запрос на создание бронирования для пользователя с ID: {}, данные бронирования: {}",
                bookerId, bookingReqDto);
        BookingSendDto result = service.create(bookingReqDto, bookerId);
        log.info("Бронирование успешно создано для пользователя с ID: {}, ID бронирования: {}",
                bookerId, result.getId());
        return result;
    }

    /**
     * Подтверждает или отклоняет запрос на бронирование (только для владельца вещи).
     *
     * @param bookingId ID бронирования
     * @param approved  true для APPROVED, false для REJECTED
     * @param userId    ID пользователя (владельца вещи)
     * @return DTO с данными обновлённого бронирования
     */
    @PatchMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    public BookingSendDto approveOrRejectBooking(
            @PathVariable @Positive(message = "ID брони должен быть положительным числом") Long bookingId,
            @RequestParam boolean approved,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        String action = approved ? "одобрение" : "отклонение";
        log.info("Получен запрос на {} бронирования с ID: {} от пользователя с ID: {}",
                action, bookingId, userId);
        BookingSendDto result = service.approveOrRejectBooking(bookingId, approved, userId);
        log.info("Бронирование с ID: {} успешно {} для пользователя с ID: {}",
                bookingId, action, userId);
        return result;
    }

    /**
     * Возвращает информацию о конкретном бронировании.
     * Доступ разрешён только Booker (создателю брони) или Owner (владельцу вещи).
     */
    @GetMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    public BookingSendDto getBooking(
            @PathVariable @Positive(message = "ID брони должен быть положительным числом") Long bookingId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        log.info("Получен запрос на получение информации о бронировании с ID: {} для пользователя с ID: {}",
                bookingId, userId);
        BookingSendDto result = service.getByIdForBookerOrOwner(bookingId, userId);
        log.info("Информация о бронировании с ID: {} успешно получена для пользователя с ID: {}",
                bookingId, userId);
        return result;
    }

    /**
     * Возвращает бронирования пользователя с фильтрацией по состоянию.
     * По умолчанию возвращает все бронирования (state=ALL).
     * Поддерживаемые состояния: ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getBookings(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId,
            @RequestParam(defaultValue = "ALL") String state) {
        log.info("Получен запрос на получение бронирований для пользователя с ID: {}, состояние: {}",
                userId, state);
        Collection<BookingSendDto> result = service.getBookingsByState(userId, state);
        log.info("Найдено {} бронирований для пользователя с ID: {}, состояние: {}",
                result.size(), userId, state);
        return result;
    }

    /**
     * Возвращает бронирования для всех вещей текущего пользователя (владельца).
     * Доступ только для владельца вещей.
     */
    @GetMapping("/owner")
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getOwnerBookings(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long ownerId,
            @RequestParam(defaultValue = "ALL") String state) {
        log.info("Получен запрос на получение бронирований вещей для владельца с ID: {}, состояние: {}",
                ownerId, state);
        Collection<BookingSendDto> result = service.getBookingsByOwnerState(ownerId, state);
        log.info("Найдено {} бронирований вещей для владельца с ID: {}, состояние: {}",
                result.size(), ownerId, state);
        return result;
    }
}
