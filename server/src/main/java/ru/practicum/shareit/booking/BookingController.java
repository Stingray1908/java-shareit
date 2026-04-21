package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.HttpHeader;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingSendDto addBooking(
            @RequestBody BookingReqDto bookingReqDto,
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long bookerId) {
        log.info("Сервер: получен запрос на создание бронирования для пользователя с ID: {}, данные бронирования: {}",
                bookerId, bookingReqDto);
        BookingSendDto result = bookingService.create(bookingReqDto, bookerId);
        log.info("Сервер: бронирование успешно создано для пользователя с ID: {}, ID бронирования: {}",
                bookerId, result.getId());
        return result;
    }

    @PatchMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    public BookingSendDto approveOrRejectBooking(
            @PathVariable Long bookingId,
            @RequestParam boolean approved,
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        String action = approved ? "одобрение" : "отклонение";
        log.info("Сервер: получен запрос на {} бронирования с ID: {} от пользователя с ID: {}",
                action, bookingId, userId);
        BookingSendDto result = bookingService.approveOrRejectBooking(bookingId, approved, userId);
        log.info("Сервер: бронирование с ID: {} успешно {} для пользователем с ID: {}",
                bookingId, action, userId);
        return result;
    }

    @GetMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    public BookingSendDto getBooking(
            @PathVariable Long bookingId,
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        log.info("Сервер: получен запрос на получение информации о бронировании с ID: {} для пользователя с ID: {}",
                bookingId, userId);
        BookingSendDto result = bookingService.getByIdForBookerOrOwner(bookingId, userId);
        log.info("Сервер: информация о бронировании с ID: {} успешно получена для пользователя с ID: {}",
                bookingId, userId);
        return result;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getBookings(
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Сервер: получен запрос на получение бронирований для пользователя с ID: {}, состояние: {}, from: {}, size: {}",
                userId, state, from, size);
        Collection<BookingSendDto> result = bookingService.getBookingsByState(userId, state);
        log.info("Сервер: найдено {} бронирований для пользователя с ID: {}, состояние: {}",
                result.size(), userId, state);
        return result;
    }

    @GetMapping("/owner")
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getOwnerBookings(
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long ownerId,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Сервер: получен запрос на получение бронирований вещей для владельца с ID: {}, состояние: {}, from: {}, size: {}",
                ownerId, state, from, size);
        Collection<BookingSendDto> result = bookingService.getBookingsByOwnerState(ownerId, state);
        log.info("Сервер: найдено {} бронирований вещей для владельца с ID: {}, состояние: {}",
                result.size(), ownerId, state);
        return result;
    }
}
