package ru.practicum.shareit.booking.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.common.enums.BookingState;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service("BookingJpaService")
public class BookingJpaService implements BookingService {

    private final BookingJpaRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;
    private final BookingMapper bookingMapper = new BookingMapper();
    private final UserMapper userMapper = new UserMapper();
    private final ItemMapper itemMapper = new ItemMapper();

    @Override
    @Transactional
    public BookingSendDto create(BookingReqDto dto, Long bookerId) {
        User booker = userService.getByIdOrThrowInternal(bookerId);
        Item item = itemService.getByIdOrThrowInternal(dto.getItemId());

        validateBooking(dto, bookerId, item);

        Booking booking = bookingMapper.toEntity(dto);
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        return toSendDto(bookingRepository.save(booking));
    }

    private void validateBooking(BookingReqDto dto, Long bookerId, Item item) {
        if (Objects.equals(item.getOwner().getId(), bookerId))
            throw new IllegalArgumentException("Пользователь не может бронировать свои вещи");

        if (!item.getAvailable())
            throw new IllegalArgumentException("Вещь недоступна (available = false)");

        if (bookingRepository.checkOverLapBookings(item.getId(), dto.getStart(), dto.getEnd()))
            throw new IllegalArgumentException("Время уже забронировано");

        validateDates(dto.getStart(), dto.getEnd());
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end))
            throw new IllegalArgumentException("Время начала не может быть после времени окончания");

        if (start.equals(end))
            throw new IllegalArgumentException("Время начала и окончания брони не может совпадать");

        if (start.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Время начала должно быть в будущем");
    }

    @Override
    @Transactional
    public BookingSendDto approveOrRejectBooking(Long bookingId, boolean approved, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Бронь с Id: " + bookingId + " не найдена"));

        Long itemOwnerId = booking.getItem().getOwner().getId();
        if (!Objects.equals(itemOwnerId, userId)) {
            throw new SecurityException(
                    String.format("Пользователь Id: %d не является владельцем вещи и не может подтверждать/отклонять бронь", userId));
        }

        if (!booking.getStatus().equals(BookingStatus.WAITING)) {
            throw new IllegalArgumentException(
                    String.format("Можно подтверждать/отклонять только бронирования со статусом WAITING, текущий статус: %s",
                            booking.getStatus().name()));
        }

        BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        booking.setStatus(newStatus);

        return toSendDto(bookingRepository.save(booking));
    }

    @Override
    public BookingSendDto getByIdForBookerOrOwner(Long bookingId, Long userId) {
        Booking booking = findByIdOrThrowInternal(bookingId);
        Long bookerId = booking.getBooker().getId();

        if (!Objects.equals(userId, bookerId)) {
            Long ownerId = booking.getItem().getOwner().getId();
            if (!Objects.equals(userId, ownerId)) {
                throw new SecurityException(String.format("Пользователь Id: %d пытается получить бронь пользователя Id: %d",
                        userId, bookerId));
            }
        }
        return toSendDto(booking);
    }

    @Override
    public Collection<BookingSendDto> getBookingsByState(Long userId, String state) {
        userService.getByIdOrThrowInternal(userId);
        BookingState bookingState = parseBookingState(state);

        List<Booking> bookings = findBookingsByStateAndUser(userId, bookingState, false);;
        sortBookingsDescending(bookings);

        return toListSendDto(bookings);
    }

    @Override
    public Collection<BookingSendDto> getBookingsByOwnerState(Long ownerId, String state) {
        userService.getByIdOrThrowInternal(ownerId);
        BookingState bookingState = parseBookingState(state);

        List<Booking> bookings = findBookingsByStateAndUser(ownerId, bookingState, true);
        sortBookingsDescending(bookings);

        return toListSendDto(bookings);
    }

    private void sortBookingsDescending(List<Booking> bookings) {
        bookings.sort((b1, b2) -> b2.getStart().compareTo(b1.getStart()));
    }

    private BookingState parseBookingState(String state) {
        try {
            return BookingState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    ("Недопустимое значение state: " + state +
                            ". Допустимые значения: ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED"));
        }
    }

    private List<Booking> findBookingsByStateAndUser(Long userId, BookingState state, boolean forOwner) {
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL:
                return forOwner
                        ? bookingRepository.findAllByItemOwnerId(userId)
                        : bookingRepository.findAllByBookerId(userId);
            case CURRENT:
                return forOwner
                        ? bookingRepository.findCurrentBookingsForOwner(userId, now)
                        : bookingRepository.findCurrentBookings(userId, now);
            case PAST:
                return forOwner
                        ? bookingRepository.findPastBookingsForOwner(userId, now)
                        : bookingRepository.findPastBookings(userId, now);
            case FUTURE:
                return forOwner
                        ? bookingRepository.findFutureBookingsForOwner(userId, now)
                        : bookingRepository.findFutureBookings(userId, now);
            case WAITING:
            case REJECTED:
                BookingStatus status = state == BookingState.WAITING
                        ? BookingStatus.WAITING
                        : BookingStatus.REJECTED;
                return forOwner
                        ? bookingRepository.findByItemOwnerIdAndStatus(userId, status)
                        : bookingRepository.findByBookerIdAndStatus(userId, status);
            default:
                throw new IllegalArgumentException("Неподдерживаемое состояние: " + state);
        }
    }


    @Override
    public Booking findByIdOrThrowInternal(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Бронь с Id: " + id + " не существует"));
    }

    private BookingSendDto toSendDto(Booking booking) {
        BookingSendDto dto = bookingMapper.toSendDto(booking);
        dto.setBooker(userMapper.toSendDto(booking.getBooker()));
        dto.setItem(itemMapper.toSendDto(booking.getItem()));
        return dto;
    }

    private List<BookingSendDto> toListSendDto(Collection<Booking> bookings) {
        return bookings.stream()
                .map(this::toSendDto)
                .toList();
    }
}
