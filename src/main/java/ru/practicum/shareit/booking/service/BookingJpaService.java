package ru.practicum.shareit.booking.service;

import io.micrometer.core.instrument.config.validate.ValidationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserService;

import java.awt.print.Book;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@Service("BookingJpaService")
public class BookingJpaService implements BookingService {

    @Autowired
    private BookingJpaRepository bookingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    final private BookingMapper bookingMapper;

    // Константы статусов
    private static final Set<BookingStatus> ACTIVE_STATUSES = Set.of(
            BookingStatus.WAITING,
            BookingStatus.APPROVED
    );

    private static final Set<BookingStatus> FINAL_STATUSES = Set.of(
            BookingStatus.CANCELLED,
            BookingStatus.COMPLETED,
            BookingStatus.REJECTED
    );

    // Правила переходов между статусами
    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
            BookingStatus.WAITING, Set.of(BookingStatus.APPROVED, BookingStatus.REJECTED, BookingStatus.CANCELLED),
            BookingStatus.APPROVED, Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
    );


    @Override
    @Transactional
    public BookingSendDto create(BookingReqDto dto) {
        System.out.println(bookingRepository.findAll());
        Booking booking = bookingMapper.toEntity(dto);
        validateBookingDate(booking);

        User booker = userService.getByIdOrThrowInternal(dto.getBookerId());
        Item item = itemService.getByIdOrThrowInternal(dto.getItemId());

        if (Objects.equals(item.getOwner().getId(), dto.getBookerId()))
            throw new IllegalArgumentException("Пользователь не может бронировать свои вещи");

        if (!item.getAvailable()) {
            throw new IllegalArgumentException("Вещь недоступна (available = false)");
        }

        if (bookingRepository.checkOverLapBookings(item.getId(), booking.getStart(), booking.getEnd())) {
            throw new IllegalArgumentException("время уже забронировано");
        }

        System.out.println(bookingRepository.findAll());

        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        Booking created = bookingRepository.save(booking);
        BookingSendDto response = bookingMapper.toSendDto(created);
        response.setBooker(new UserMapper().toSendDto(booker));
        response.setItem(new ItemMapper().toSendDto(item));
        return response;
    }

    private void validateBookingDate(Booking booking) {

        if (booking.getStart().isAfter(booking.getEnd()))
            throw new IllegalArgumentException("Время начала не может быть после старта");

        if (booking.getStart().equals(booking.getEnd()))
            throw new IllegalArgumentException("Время начала окончания брони не может совпадать");

        if (booking.getStart().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Время начала должно быть в будущем");
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus newStatus) {
        if (FINAL_STATUSES.contains(current)) {
            throw new SecurityException(
                    String.format("Статус %s является финальным и не может быть изменён", current.name()));
        }

        Set<BookingStatus> allowedTransitions = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedTransitions.contains(newStatus)) {
            throw new SecurityException(
                    String.format("Недопустимый переход из статуса %s в статус %s", current.name(), newStatus.name()));
        }
    }

    private void validateStatusChangePermissions(
            Booking booking,
            Long userId,
            Long bookerId,
            Long itemOwnerId,
            BookingStatus newStatus
    ) {
        boolean isOwner = Objects.equals(itemOwnerId, userId);
        boolean isBooker = Objects.equals(bookerId, userId);

        if (!isOwner && !isBooker) {
            throw new SecurityException(
                    String.format("Пользователь Id: %d не имеет прав на изменение брони Id: %d", userId, booking.getId()));
        }

        if (isBooker && !newStatus.equals(BookingStatus.CANCELLED)) {
            throw new SecurityException(
                    String.format("Пользователь Id: %d может установить только статус CANCELLED для своей брони", userId));
        }

        if (isOwner && (newStatus.equals(BookingStatus.WAITING) || newStatus.equals(BookingStatus.CANCELLED))) {
            throw new SecurityException(
                    String.format("Владелец вещи Id: %d не может установить статус %s для брони Id: %d",
                            itemOwnerId, newStatus.name(), booking.getId()));
        }
    }

   @Override
    public BookingSendDto patchBooking(BookingReqDto reqDto, Long userId) {

        Booking booking = bookingRepository.findByIdWithActiveStatus(reqDto.getId());
        if (booking == null) {
            throw new IllegalArgumentException("Бронь с Id: " + reqDto.getId() + " с активным статусом не найдена");
        }

        BookingStatus currentStatus = booking.getStatus();
        BookingStatus newStatus = reqDto.getStatus();

        // Оптимизация: если статус не изменился
        if (currentStatus.equals(newStatus)) {
            return bookingMapper.toSendDto(booking);
        }

        Long bookerId = booking.getBooker().getId();
        Long itemOwnerId = booking.getItem().getOwner().getId();

        validateStatusChangePermissions(booking, userId, bookerId, itemOwnerId, newStatus);
        validateStatusTransition(currentStatus, newStatus);

        booking.setStatus(newStatus);
        return bookingMapper.toSendDto(bookingRepository.save(booking));
    }

    @Override
    public Booking findByIdOrThrowInternal(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("бронь с Id: "+id+ " не существует"));
    }

    @Override
    public void deleteBooking(Long bookingId, Long bookerId) {

    }

    @Override
    public BookingSendDto getBooking(Long bookingId, Long userId) {
        return null;
    }

    @Override
    public Collection<BookingSendDto> getCreatedBookings(Long userId) {
        return List.of();
    }

    @Override
    public Collection<Booking> getItemBookingsInternal(Long itemId) {
        return List.of();
    }

    @Override
    public Collection<BookingSendDto> getItemBookingsExternal(Long itemId) {
        return List.of();
    }
}
