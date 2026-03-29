/*package ru.practicum.shareit.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {
    BookingRepository bookingRepository;
    UserService userService;
    ItemService itemService;
    BookingMapper mapper;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              UserService userService,
                              ItemService itemService,
                              BookingMapper mapper) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.itemService = itemService;
        this.mapper = mapper;
    }

    @Override
    public BookingSendDto addBooking(BookingReqDto reqDto) {
        Booking booking = mapper.toEntity(reqDto);

        // проверка корректности обязательных полей
        validateBookingCreation(booking);

        //проверка существования User и Item
        User user;
        Item item;
        if ((user = userService.getByIdInternal(booking.getBookerId())) == null) {
            throw new NoSuchElementException("Пользователь с ID: " + booking.getBookerId() + " не найдена");
        }
        if ((item = itemService.getByIdInternal(booking.getItemId())) == null) {
            throw new NoSuchElementException("Вещь с ID: " + booking.getItemId() + " не найдена");
        }
        //if (Objects.equals(user.getId(), item.getOwnerId())) {
          //  throw new IllegalArgumentException("Владелец не может бронировать свои же вещи");
        //}

        if (!item.getAvailable()) {
            throw new IllegalArgumentException("Вещь недоступна (available = false)");
        }

        // Получаю брони для вещи
        NavigableSet<Booking> bookings = bookingRepository.getItemBookings(booking.getItemId());

        if (!bookings.isEmpty()) {
            // Проверяем имеется ли уже активная бронь от этого пользователя на эту вещь
            isItemAlreadyBookedByUser(bookings, booking);

            // Проверяем пересекается ли новая бронь с имеющимися по времени
            isOverlapping(bookings, booking);
        }

        booking.setStatus(BookingStatus.WAITING);

        Booking created = bookingRepository.addBooking(booking);
        return mapper.toSendDto(created);
    }

    @Override
    public BookingSendDto patchBooking(BookingReqDto reqDto, Long userId) {
        Long bookingId = reqDto.getId();
        BookingStatus newStatus = reqDto.getStatus();

        // Получаем бронь и проверяем существование
        Booking existing = bookingRepository.findBooking(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Бронь с ID: " + bookingId + " не найдена"));

        // Проверяем, что бронь активна
        validateActiveBooking(existing);

        // Проверяем доступ пользователя к брони
        checkAccessToBooking(existing, userId);

        Long bookerId = existing.getBookerId();
        boolean isBooker = Objects.equals(bookerId, userId);

        if (isBooker) {
            // Букер может только отменить бронь
            if (newStatus != BookingStatus.CANCELLED) {
                throw new IllegalArgumentException("Booker может только отменить бронь (статус CANCELED)");
            }
            existing.setStatus(newStatus);
        } else {
            // Владелец: проверяем правила изменения статуса
            validateOwnerStatusChange(existing.getStatus(), newStatus);
            existing.setStatus(newStatus);
        }

        return mapper.toSendDto(bookingRepository.patchBooking(existing));
    }

    @Override
    public void deleteBooking(Long bookingId, Long bookerId) {
        // Получаем бронь из репозитория
        Booking existingBooking = bookingRepository.findBooking(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Бронь с ID: " + bookingId + " не найдена"));

        // Проверяем, что пользователь, запрашивающий удаление, является Booker этой брони
        if (!Objects.equals(existingBooking.getBookerId(), bookerId)) {
            throw new IllegalArgumentException(
                    "Только создатель брони (Booker) может её удалить. " +
                            "Текущий пользователь ID: " + bookerId +
                            ", создатель брони ID: " + existingBooking.getBookerId()
            );
        }

        // Выполняем удаление брони через репозиторий, передавая объект
        bookingRepository.deleteBooking(existingBooking);
        log.debug("Бронь с ID: {} успешно удалена пользователем ID: {}", bookingId, bookerId);
    }

    @Override
    public BookingSendDto getBooking(Long bookingId, Long userId) {
        // Проверяем существование и доступ — бронь найдена внутри метода
        Booking booking = bookingRepository.findBooking(bookingId)
                .orElseThrow(
                        () -> new NoSuchElementException("Нет брони с ID: " + bookingId + " для пользователя " + userId)
                );

        checkAccessToBooking(booking, userId); // Передаём уже найденную бронь

        return mapper.toSendDto(booking);
    }

    @Override
    public Collection<BookingSendDto> getCreatedBookings(Long userId) {
        if (userService.getByIdInternal(userId) == null) {
            throw new NoSuchElementException("пользователь с ID:" + userId + " не существует");
        }

        Collection<Booking> bookings = bookingRepository.getCreatedBookings(userId);
        if (bookings.isEmpty()) {
            return new ArrayList<>();
        }

        return bookings.stream()
                .map(mapper::toSendDto)
                .toList();
    }

    @Override
    public Collection<Booking> getItemBookingsInternal(Long itemId) {
        itemService.getByIdInternal(itemId);
        return bookingRepository.getItemBookings(itemId).stream()
                .toList();
    }

    @Override
    public Collection<BookingSendDto> getItemBookingsExternal(Long itemId) {
        return getItemBookingsInternal(itemId).stream()
                .map(mapper::toSendDto)
                .toList();
    }


    private void validateActiveBooking(Booking booking) {
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.REJECTED ||
                status == BookingStatus.CANCELLED ||
                status == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Нельзя изменить статус завершённой брони");
        }
    }

    private void checkAccessToBooking(Booking booking, Long userId) {
        if (Objects.equals(userId, booking.getBookerId())) {
            return;
        }

        ItemSendDTO item = itemService.getById(booking.getItemId());
        if (!Objects.equals(item.getOwnerId(), userId)) {
            throw new SecurityException("Пользователь с ID: " + userId + " не имеет доступа к брони с ID: " + booking.getId());
        }
    }

    private void validateBookingCreation(Booking booking) {
        if (booking.getBookerId() == null
                || booking.getItemId() == null
                || booking.getStart() == null
                || booking.getEnd() == null
        ) throw new IllegalArgumentException("Необходимо заполнить все обязательные поля");

        if (booking.getStart().isAfter(booking.getEnd()))
            throw new IllegalArgumentException("Время начала не может быть после старта");

        if (booking.getStart().equals(booking.getEnd()))
            throw new IllegalArgumentException("Время начала окончания брони не может совпадать");

        if (booking.getStart().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Время начала должно быть в будущем");
    }


    private void isItemAlreadyBookedByUser(NavigableSet<Booking> bookings, Booking booking) {
        boolean hasActiveBooking = bookings.stream()
                .anyMatch(b ->
                        Objects.equals(b.getBookerId(), booking.getBookerId()) &&
                                (b.getStatus() == BookingStatus.WAITING ||
                                        b.getStatus() == BookingStatus.APPROVED)
                );

        if (hasActiveBooking) {
            throw new IllegalArgumentException(
                    "Бронирование невозможно: вещь с ID " + booking.getItemId() +
                            " уже имеет активный статус"
            );
        }
    }



    private void isOverlapping(NavigableSet<Booking> bookings, Booking booking) {
        LocalDateTime newStart = booking.getStart();
        LocalDateTime newEnd = booking.getEnd();

        for (Booking existing : bookings) {
            LocalDateTime existingStart = existing.getStart();
            LocalDateTime existingEnd = existing.getEnd();

            // Условие пересечения: новое бронирование пересекается с существующим
            boolean isOverlapping =
                    (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) ||
                            (newStart.isEqual(existingEnd)) ||  // Конец нового = начало существующего
                            (newEnd.isEqual(existingStart));   // Начало нового = конец существующего

            if (isOverlapping) {
                DateTimeFormatter ofError = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

                throw new IllegalArgumentException(String.format(
                        "Пересечение с существующим бронированием: новое (%s–%s) пересекается с (%s–%s)",
                        newStart.format(ofError),
                        newEnd.format(ofError),
                        existingStart.format(ofError),
                        existingEnd.format(ofError)
                ));
            }
        }
    }


    private void validateOwnerStatusChange(BookingStatus currentStatus, BookingStatus newStatus) {
        switch (currentStatus) {
            case WAITING:
                // Из ожидания владелец может подтвердить или отклонить
                if (newStatus != BookingStatus.APPROVED && newStatus != BookingStatus.REJECTED) {
                    throw new IllegalArgumentException(
                            "Owner может из статуса 'ожидает одобрения' установить только 'подтверждено' или 'отклонено'");
                }
                break;
            case APPROVED:
                // Из подтверждённого владелец может только завершить (не может отклонить)
                if (newStatus != BookingStatus.COMPLETED) {
                    throw new IllegalArgumentException(
                            "Owner не может изменить статус 'подтверждено' на '" + newStatus.name() + "'. Разрешён только 'завершено'");
                }
                break;
            default:
                // Для других статусов (уже завершённых) изменения запрещены
                throw new IllegalArgumentException("Нельзя изменить статус брони в текущем состоянии");
        }
    }
}

*/
