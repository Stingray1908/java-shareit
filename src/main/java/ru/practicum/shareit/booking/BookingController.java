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
            @RequestHeader(HttpHeader.X_BOOKER_USER_ID) Long bookerId) {
        return service.create(bookingReqDto, bookerId);
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
            @RequestHeader(HttpHeader.X_USER_ID) Long userId) {
        return service.approveOrRejectBooking(bookingId, approved, userId);
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
            @RequestHeader(HttpHeader.X_USER_ID) Long userId) {
        return service.getByIdForBookerOrOwner(bookingId, userId);
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
            @RequestHeader(HttpHeader.X_USER_ID) Long userId,
            @RequestParam(defaultValue = "ALL") String state) {
        return service.getBookingsByState(userId, state);
    }

    /**
     * Возвращает бронирования для всех вещей текущего пользователя (владельца).
     * Доступ только для владельца вещей.
     */
    @GetMapping("/owner")
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getOwnerBookings(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_USER_ID) Long ownerId,
            @RequestParam(defaultValue = "ALL") String state) {
        return service.getBookingsByOwnerState(ownerId, state);
    }
}
