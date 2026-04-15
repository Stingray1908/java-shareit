package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.common.constants.HttpHeader;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<Object> addBooking(
            @RequestBody @Valid BookingReqDto bookingReqDto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long bookerId) {
        log.info("Ворота: получен запрос на создание бронирования для пользователя с ID: {}, данные бронирования: {}",
                bookerId, bookingReqDto);
        ResponseEntity<Object> result = bookingClient.bookItem(bookerId, bookingReqDto);
        log.info("Ворота: ответ от сервера получен для пользователя с ID: {}", bookerId);
        return result;
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> approveOrRejectBooking(
            @PathVariable @Positive(message = "ID брони должен быть положительным числом") Long bookingId,
            @RequestParam boolean approved,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        String action = approved ? "одобрение" : "отклонение";
        log.info("Ворота: получен запрос на {} бронирования с ID: {} от пользователя с ID: {}",
                action, bookingId, userId);
        ResponseEntity<Object> result = bookingClient.approveOrRejectBooking(bookingId, approved, userId);
        log.info("Ворота: бронирование с ID: {} успешно {} для пользователя с ID: {}",
                bookingId, action, userId);
        return result;
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getBooking(
            @PathVariable @Positive(message = "ID брони должен быть положительным числом") Long bookingId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId) {
        log.info("Ворота: получен запрос на получение информации о бронировании с ID: {} для пользователя с ID: {}",
                bookingId, userId);
        ResponseEntity<Object> result = bookingClient.getBooking(userId, bookingId);
        log.info("Ворота: информация о бронировании с ID: {} успешно получена для пользователя с ID: {}",
                bookingId, userId);
        return result;
    }

    @GetMapping
    public ResponseEntity<Object> getBookings(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long userId,
            @RequestParam(defaultValue = "ALL") String state) {
        log.info("Ворота: получен запрос на получение бронирований для пользователя с ID: {}, состояние: {}",
                userId, state);
        ResponseEntity<Object> result = bookingClient.getBookings(userId, state, 0, 10);
        log.info("Ворота: ответ от сервера получен для пользователя с ID: {}", userId);
        return result;
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getOwnerBookings(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long ownerId,
            @RequestParam(defaultValue = "ALL") String state) {
        log.info("Ворота: получен запрос на получение бронирований вещей для владельца с ID: {}, состояние: {}",
                ownerId, state);
        ResponseEntity<Object> result = bookingClient.getOwnerBookings(ownerId, state, 0, 10);
        log.info("Ворота: ответ от сервера получен для владельца с ID: {}", ownerId);
        return result;
    }
}
