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
import ru.practicum.shareit.common.groups.OnCreate;
import ru.practicum.shareit.common.groups.OnUpdate;

import java.util.Collection;

/**
 * REST‑контроллер для управления бронированиями вещей.
 * Предоставляет API‑методы для создания, обновления, получения и удаления бронирований.
 * <p>
 * Основные правила бизнес‑логики:
 * - При создании статус брони всегда устанавливается как {@code WAITING}.
 * - Пользователь не может бронировать свои собственные вещи.
 * - Вещь должна быть доступна для бронирования ({@code available = true}).
 * - Запрещено создавать пересекающиеся бронирования для одной вещи.
 * - Время начала брони должно быть в будущем и меньше времени окончания.
 * - Пользователь не может иметь несколько активных бронирований одной вещи.
 * - {@code Booker} (создатель брони) может:
 * - отменить бронь (статус {@code CANCELED});
 * - удалить бронь.
 * - {@code Owner} (владелец вещи) может устанавливать статусы:
 * - из {@code WAITING} → {@code APPROVED} или {@code REJECTED};
 * - из {@code APPROVED} → {@code COMPLETED};
 * - нельзя изменить статус для {@code REJECTED}, {@code CANCELED}, {@code COMPLETED}.
 * - Доступ к просмотру брони имеют только {@code Booker} или {@code Owner}.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/bookings")
public class BookingController {

    BookingService service;

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
    public BookingSendDto addBooking(@RequestBody
                                     @Validated(OnCreate.class) BookingReqDto bookingReqDto,
                                     @Positive(message = "ID пользователя должен быть положительным числом")
                                     @RequestHeader(HttpHeader.X_BOOKER_USER_ID) Long bookerId) {
        bookingReqDto.setBookerId(bookerId);
        return service.addBooking(bookingReqDto);
    }

    /**
     * Обновляет статус существующего бронирования.
     * Booker может установить только CANCELED. Owner — APPROVED, REJECTED или COMPLETED.
     * Проверяет права доступа и ограничения по статусам (например, после APPROVED возможен только COMPLETED).
     */
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookingSendDto patchBooking(@RequestBody @Validated(OnUpdate.class) BookingReqDto bookingReqDto,

                                       @PathVariable @Positive(message = "ID брони должен быть положительным числом")
                                       Long id,
                                       @Positive(message = "ID пользователя должен быть положительным числом")
                                       @RequestHeader(HttpHeader.X_USER_ID) Long userId) {
        bookingReqDto.setId(id);
        return service.patchBooking(bookingReqDto, userId);
    }

    /**
     * Возвращает информацию о конкретном бронировании.
     * Доступ разрешён только Booker (создателю брони) или Owner (владельцу вещи).
     */
    @GetMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    public BookingSendDto getBooking(@Positive(message = "ID пользователя должен быть положительным числом")
                                     @PathVariable Long bookingId,

                                     @Positive(message = "ID пользователя должен быть положительным числом")
                                     @RequestHeader(HttpHeader.X_USER_ID) Long booker) {
        return service.getBooking(bookingId, booker);
    }

    /**
     * Возвращает список всех бронирований для указанной вещи.
     * Метод общедоступный — не требует специальных прав доступа.
     */
    @GetMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getBooking(@Positive(message = "ID вещи должен быть положительным числом")
                                                 @PathVariable Long itemId) {
        return service.getItemBookingsExternal(itemId);
    }

    /**
     * Возвращает все бронирования, созданные указанным пользователем.
     * Если бронирований нет, возвращает пустой массив. Проверяет существование пользователя.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<BookingSendDto> getCreatedBookings(@Positive(message = "ID пользователя должен быть положительным числом")
                                                         @RequestHeader(HttpHeader.X_USER_ID) Long id) {
        return service.getCreatedBookings(id);
    }

    /**
     * Удаляет существующее бронирование.
     * Доступно только для Booker — создателя брони. Проверяет существование бронирования.
     */
    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteBooking(@Positive(message = "ID пользователя должен быть положительным числом")
                              @PathVariable Long bookingId,

                              @Positive(message = "ID пользователя должен быть положительным числом")
                              @RequestHeader(HttpHeader.X_BOOKER_USER_ID) Long booker) {
        service.deleteBooking(bookingId, booker);
    }

}
