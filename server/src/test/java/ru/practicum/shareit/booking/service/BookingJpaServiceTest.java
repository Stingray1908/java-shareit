package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingJpaServiceTest {

    @Mock
    private BookingJpaRepository bookingRepository;
    @Mock
    private UserService userService;
    @Mock
    private ItemService itemService;

    @InjectMocks
    private BookingJpaService bookingService;

    private User booker;
    private User owner;
    private Item item;
    private Booking booking;

    @BeforeEach
    void setUp() {
        booker = new User();
        booker.setId(1L);
        booker.setName("Booker");
        booker.setEmail("booker@test.com");

        owner = new User();
        owner.setId(2L);
        owner.setName("Owner");
        owner.setEmail("owner@test.com");

        item = new Item();
        item.setId(10L);
        item.setName("Test Item");
        item.setAvailable(true);
        item.setOwner(owner);

        booking = new Booking();
        booking.setId(100L);
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);
        booking.setStart(LocalDateTime.now().plusHours(2));
        booking.setEnd(LocalDateTime.now().plusHours(4));
    }

    @Test
    void create_ShouldCreateBooking_WhenValidData() {
        // Given
        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(itemService.getByIdOrThrowInternal(item.getId())).thenReturn(item);
        when(bookingRepository.checkOverLapBookings(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any())).thenReturn(booking);

        // When
        BookingSendDto result = bookingService.create(dto, booker.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    void create_ShouldThrowException_WhenUserTriesToBookOwnItem() {
        // Given
        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());

        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(itemService.getByIdOrThrowInternal(item.getId())).thenReturn(item);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, owner.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь не может бронировать свои вещи");
    }

    @Test
    void create_ShouldThrowException_WhenItemIsUnavailable() {
        // Given
        item.setAvailable(false);

        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(itemService.getByIdOrThrowInternal(item.getId())).thenReturn(item);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, booker.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Вещь недоступна (available = false)");
    }

    @Test
    void approveOrRejectBooking_ShouldApprove_WhenValidRequest() {
        // Given
        when(bookingRepository.findByIdWithAssociations(booking.getId()))
                .thenReturn(Optional.of(booking));

        // When
        BookingSendDto result = bookingService.approveOrRejectBooking(booking.getId(), true, owner.getId());

        // Then
        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
        verify(bookingRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void approveOrRejectBooking_ShouldThrowException_WhenNotOwner() {
        // Given
        when(bookingRepository.findByIdWithAssociations(booking.getId()))
                .thenReturn(Optional.of(booking));

        // When & Then
        assertThatThrownBy(() ->
                bookingService.approveOrRejectBooking(booking.getId(), true, booker.getId()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getByIdForBookerOrOwner_ShouldReturnBooking_WhenUserIsBooker() {
        // Given
        when(bookingRepository.findByIdWithAssociations(booking.getId()))
                .thenReturn(Optional.of(booking));

        // When
        BookingSendDto result = bookingService.getByIdForBookerOrOwner(booking.getId(), booker.getId());

        // Then
        assertThat(result.getId()).isEqualTo(booking.getId());
    }

    @Test
    void getBookingsByState_ShouldReturnAllBookings_WhenStateIsAll() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findAllByBookerId(booker.getId())).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "ALL");

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void parseBookingState_ShouldThrowException_ForInvalidState() {
        // When & Then
        assertThatThrownBy(() -> bookingService.getBookingsByState(booker.getId(), "INVALID_STATE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Недопустимое значение state: INVALID_STATE");
    }

    @Test
    void validateDates_ShouldThrowException_WhenStartAfterEnd() {
        // Given
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        LocalDateTime end = LocalDateTime.now().plusHours(3);

        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(start);
        dto.setEnd(end);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(itemService.getByIdOrThrowInternal(item.getId())).thenReturn(item);
        when(bookingRepository.checkOverLapBookings(any(), any(), any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, booker.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Время начала не может быть после времени окончания");
    }

    @Test
    void validateDates_ShouldThrowException_WhenStartEqualsEnd() {
        // Given
        LocalDateTime sameTime = LocalDateTime.now().plusHours(2);

        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(sameTime);
        dto.setEnd(sameTime);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(itemService.getByIdOrThrowInternal(item.getId())).thenReturn(item);
        when(bookingRepository.checkOverLapBookings(any(), any(), any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, booker.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Время начала и окончания брони не может совпадать");
    }

    @Test
    void findByIdOrThrowInternal_ShouldThrowException_WhenBookingNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(bookingRepository.findByIdWithAssociations(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.findByIdOrThrowInternal(nonExistentId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Бронь с Id: " + nonExistentId + " не существует");
    }

    @Test
    void getBookingsByOwnerState_ShouldReturnBookingsForOwner_WhenStateIsAll() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findAllByItemOwnerId(owner.getId())).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "ALL");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findAllByItemOwnerId(owner.getId());
    }

    @Test
    void approveOrRejectBooking_ShouldThrowException_WhenStatusNotWaiting() {
        // Given
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findByIdWithAssociations(booking.getId()))
                .thenReturn(Optional.of(booking));

        // When & Then
        assertThatThrownBy(() ->
                bookingService.approveOrRejectBooking(booking.getId(), true, owner.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Можно подтверждать/отклонять только бронирования со статусом WAITING");
    }

    @Test
    void getByIdForBookerOrOwner_ShouldThrowException_WhenUnauthorizedAccess() {
        // Given
        User unauthorizedUser = new User();
        unauthorizedUser.setId(3L);

        when(bookingRepository.findByIdWithAssociations(booking.getId()))
                .thenReturn(Optional.of(booking));

        // When & Then
        assertThatThrownBy(() ->
                bookingService.getByIdForBookerOrOwner(booking.getId(), unauthorizedUser.getId()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void create_ShouldThrowException_WhenOverlappingBookingsExist() {
        // Given
        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(itemService.getByIdOrThrowInternal(item.getId())).thenReturn(item);
        when(bookingRepository.checkOverLapBookings(any(), any(), any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, booker.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Время уже забронировано");
    }

    @Test
    void bookingMapper_toSendDto_ShouldMapCorrectly() {
        // Given
        BookingMapper bookingMapper = new BookingMapper();

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStart(LocalDateTime.now().plusHours(2));
        booking.setEnd(LocalDateTime.now().plusHours(4));
        booking.setStatus(BookingStatus.WAITING);

        // When
        BookingSendDto result = bookingMapper.toSendDto(booking);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(booking.getId());
        assertThat(result.getStart()).isEqualTo(booking.getStart());
        assertThat(result.getEnd()).isEqualTo(booking.getEnd());
        assertThat(result.getStatus()).isEqualTo(booking.getStatus());
    }

    @Test
    void findBookingsByStateAndUser_ShouldReturnAllBookingsForBooker_WhenStateIsAll() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findAllByBookerId(booker.getId())).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "ALL");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findAllByBookerId(booker.getId());
    }

    @Test
    void findBookingsByStateAndUser_ShouldReturnCurrentBookingsForBooker_WhenStateIsCurrent() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findCurrentBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        )).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "CURRENT");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findCurrentBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldReturnPastBookingsForBooker_WhenStateIsPast() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findPastBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        )).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "PAST");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findPastBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldReturnFutureBookingsForBooker_WhenStateIsFuture() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findFutureBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)))
                .thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "FUTURE");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findFutureBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldReturnWaitingBookingsForBooker_WhenStateIsWaiting() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findByBookerIdAndStatus(booker.getId(), BookingStatus.WAITING))
                .thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "WAITING");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBookerIdAndStatus(booker.getId(), BookingStatus.WAITING);
    }

    @Test
    void findBookingsByStateAndUser_ShouldReturnRejectedBookingsForBooker_WhenStateIsRejected() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findByBookerIdAndStatus(booker.getId(), BookingStatus.REJECTED))
                .thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "REJECTED");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBookerIdAndStatus(booker.getId(), BookingStatus.REJECTED);
    }

    @Test
    void findBookingsByStateAndUser_ShouldReturnAllBookingsForOwner_WhenStateIsAll() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findAllByItemOwnerId(owner.getId())).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "ALL");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findAllByItemOwnerId(owner.getId());
    }

    @Test
    void findBookingsByStateAndUser_ShouldReturnCurrentBookingsForOwner_WhenStateIsCurrent() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findCurrentBookingsForOwner(
                eq(owner.getId()),
                any(LocalDateTime.class)))
                .thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "CURRENT");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findCurrentBookingsForOwner(
                eq(owner.getId()),  // ← используем eq() вместо прямого значения
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldReturnPastBookingsForOwner_WhenStateIsPast() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findPastBookingsForOwner(
                eq(owner.getId()),
                any(LocalDateTime.class)
        )).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "PAST");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findPastBookingsForOwner(
                eq(owner.getId()),
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldReturnFutureBookingsForOwner_WhenStateIsFuture() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = Collections.singletonList(booking);

        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findFutureBookingsForOwner(
                eq(owner.getId()),
                any(LocalDateTime.class)
        )).thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "FUTURE");


        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1)).findFutureBookingsForOwner(
                eq(owner.getId()),  // Используем eq() вместо raw value
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldReturnWaitingBookingsForOwner_WhenStateIsWaiting() {
        // Given
        List<Booking> bookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findByItemOwnerIdAndStatus(owner.getId(), BookingStatus.WAITING))
                .thenReturn(bookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "WAITING");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItemOwnerIdAndStatus(owner.getId(), BookingStatus.WAITING);
    }

    @Test
    void findBookingsByStateAndUser_ShouldThrowException_ForUnsupportedState() {
        // Given
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);

        // When & Then
        assertThatThrownBy(() -> bookingService.getBookingsByState(booker.getId(), "UNSUPPORTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Недопустимое значение state: UNSUPPORTED");
    }

    @Test
    void findBookingsByStateAndUser_ShouldReturnEmptyList_WhenNoBookingsExist() {
        // Given
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findFutureBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "FUTURE");

        // Then
        assertThat(result).isEmpty();
        verify(bookingRepository, times(1)).findFutureBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        );
    }


    @Test
    void findBookingsByStateAndUser_ShouldHandleOwnerAndBookerDifferentiation_Correctly() {
        // Given
        List<Booking> ownerBookings = Collections.singletonList(booking);
        List<Booking> bookerBookings = Collections.singletonList(booking);

        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);

        when(bookingRepository.findAllByItemOwnerId(owner.getId())).thenReturn(ownerBookings);
        when(bookingRepository.findAllByBookerId(booker.getId())).thenReturn(bookerBookings);

        // When: получаем бронирования как владелец
        var ownerResult = bookingService.getBookingsByOwnerState(owner.getId(), "ALL");
        // When: получаем бронирования как booker
        var bookerResult = bookingService.getBookingsByState(booker.getId(), "ALL");

        // Then: проверяем, что вызовы репозитория были разными
        verify(bookingRepository, times(1)).findAllByItemOwnerId(owner.getId());
        verify(bookingRepository, times(1)).findAllByBookerId(booker.getId());

        assertThat(ownerResult).hasSize(1);
        assertThat(bookerResult).hasSize(1);
    }

    @Test
    void findBookingsByStateAndUser_ShouldUseCurrentDateTime_ForTimeBasedStates() {
        // Given
        LocalDateTime fixedNow = LocalDateTime.of(2026, 4, 16, 14, 0, 6, 65942900); // Фиксированное время

        // Создаём бронирования с разными временными периодами
        Booking pastBooking = new Booking();
        pastBooking.setId(1L);
        pastBooking.setStart(fixedNow.minusDays(2));
        pastBooking.setEnd(fixedNow.minusDays(1));
        pastBooking.setStatus(BookingStatus.APPROVED);
        pastBooking.setBooker(booker);
        pastBooking.setItem(item);

        Booking currentBooking = new Booking();
        currentBooking.setId(2L);
        currentBooking.setStart(fixedNow.minusHours(1));
        currentBooking.setEnd(fixedNow.plusHours(1));
        currentBooking.setStatus(BookingStatus.APPROVED);
        currentBooking.setBooker(booker);
        currentBooking.setItem(item);

        Booking futureBooking = new Booking();
        futureBooking.setId(3L);
        futureBooking.setStart(fixedNow.plusDays(1));
        futureBooking.setEnd(fixedNow.plusDays(2));
        futureBooking.setStatus(BookingStatus.APPROVED);
        futureBooking.setBooker(booker);
        futureBooking.setItem(item);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findPastBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        )).thenReturn(new ArrayList<>(List.of(pastBooking)));

        when(bookingRepository.findCurrentBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        )).thenReturn(new ArrayList<>(List.of(currentBooking)));

        when(bookingRepository.findFutureBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        )).thenReturn(new ArrayList<>(List.of(futureBooking))); // Изменяемый список

        // When: проверяем разные состояния
        var pastResult = new ArrayList<>(bookingService.getBookingsByState(booker.getId(), "PAST"));
        var currentResult = new ArrayList<>(bookingService.getBookingsByState(booker.getId(), "CURRENT"));
        var futureResult = new ArrayList<>(bookingService.getBookingsByState(booker.getId(), "FUTURE"));

        // Then: проверяем корректность фильтрации по времени
        assertThat(pastResult).hasSize(1);
        assertThat(pastResult.get(0).getId()).isEqualTo(pastBooking.getId());

        assertThat(currentResult).hasSize(1);
        assertThat(currentResult.get(0).getId()).isEqualTo(currentBooking.getId());

        assertThat(futureResult).hasSize(1);
        assertThat(futureResult.get(0).getId()).isEqualTo(futureBooking.getId());
    }

    @Test
    void findBookingsByStateAndUser_ShouldIncludeCompletedInPast_WhenStateIsPast() {
        // Given
        LocalDateTime pastStart = LocalDateTime.now().minusDays(3);
        LocalDateTime pastEnd = LocalDateTime.now().minusDays(2);

        Booking completedBooking = new Booking();
        completedBooking.setId(200L);
        completedBooking.setBooker(booker);
        completedBooking.setItem(item);
        completedBooking.setStart(pastStart);
        completedBooking.setEnd(pastEnd);
        completedBooking.setStatus(BookingStatus.COMPLETED);

        List<Booking> pastBookings = Collections.singletonList(completedBooking);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findPastBookings(eq(booker.getId()), any(LocalDateTime.class)))
                .thenReturn(pastBookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "PAST");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingRepository, times(1)).findPastBookings(
                eq(booker.getId()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void findBookingsByStateAndUser_ShouldIncludeCompletedInAll_WhenStateIsAll() {
        // Given
        Booking completedBooking = new Booking();
        completedBooking.setId(300L);
        completedBooking.setBooker(booker);
        completedBooking.setItem(item);
        completedBooking.setStart(LocalDateTime.now().minusDays(1));
        completedBooking.setEnd(LocalDateTime.now());
        completedBooking.setStatus(BookingStatus.COMPLETED);

        List<Booking> allBookings = Arrays.asList(booking, completedBooking);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findAllByBookerId(booker.getId())).thenReturn(allBookings);

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "ALL");

        // Then
        assertThat(result).hasSize(2); // Включает и WAITING, и COMPLETED
        assertThat(result.stream().anyMatch(dto -> dto.getStatus() == BookingStatus.COMPLETED))
                .isTrue();
        verify(bookingRepository, times(1)).findAllByBookerId(booker.getId());
    }

    @Test
    void findBookingsByStateAndUser_ShouldNotIncludeCompletedInFuture_WhenDatesPassed() {
        // Given
        Booking completedBooking = new Booking();
        completedBooking.setId(400L);
        completedBooking.setBooker(booker);
        completedBooking.setItem(item);
        completedBooking.setStart(LocalDateTime.now().minusDays(5));
        completedBooking.setEnd(LocalDateTime.now().minusDays(4));
        completedBooking.setStatus(BookingStatus.COMPLETED);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findFutureBookings(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        var futureResult = bookingService.getBookingsByState(booker.getId(), "FUTURE");
        var currentResult = bookingService.getBookingsByState(booker.getId(), "CURRENT");

        // Then
        assertThat(futureResult).isEmpty();
        assertThat(currentResult).isEmpty();
    }

    @Test
    void getBookingsByOwnerState_ShouldReturnWaitingBookings_WhenStateIsWaiting() {
        // Given
        List<Booking> waitingBookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findByItemOwnerIdAndStatus(owner.getId(), BookingStatus.WAITING))
                .thenReturn(waitingBookings);

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "WAITING");

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItemOwnerIdAndStatus(owner.getId(), BookingStatus.WAITING);
    }

    @Test
    void getBookingsByOwnerState_ShouldReturnRejectedBookings_WhenStateIsRejected() {
        // Given
        Booking rejectedBooking = new Booking();
        rejectedBooking.setId(200L);
        rejectedBooking.setBooker(booker);
        rejectedBooking.setItem(item);
        rejectedBooking.setStatus(BookingStatus.REJECTED);
        rejectedBooking.setStart(LocalDateTime.now().plusHours(6));
        rejectedBooking.setEnd(LocalDateTime.now().plusHours(8));

        List<Booking> rejectedBookings = Collections.singletonList(rejectedBooking);

        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findByItemOwnerIdAndStatus(owner.getId(), BookingStatus.REJECTED))
                .thenReturn(rejectedBookings);

        // When
        var result = new ArrayList<>(bookingService.getBookingsByOwnerState(owner.getId(), "REJECTED"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(bookingRepository, times(1))
                .findByItemOwnerIdAndStatus(owner.getId(), BookingStatus.REJECTED);
    }

    @Test
    void getBookingsByState_ShouldReturnWaitingBookings_WhenStateIsWaiting() {
        // Given
        List<Booking> waitingBookings = Collections.singletonList(booking);
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findByBookerIdAndStatus(booker.getId(), BookingStatus.WAITING))
                .thenReturn(waitingBookings);

        // When
        var result = new ArrayList<>(bookingService.getBookingsByState(booker.getId(), "WAITING"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.WAITING);
        verify(bookingRepository, times(1))
                .findByBookerIdAndStatus(booker.getId(), BookingStatus.WAITING);
    }

    @Test
    void getBookingsByState_ShouldReturnRejectedBookings_WhenStateIsRejected() {
        // Given
        Booking rejectedBooking = new Booking();
        rejectedBooking.setId(200L);
        rejectedBooking.setBooker(booker);
        rejectedBooking.setItem(item);
        rejectedBooking.setStatus(BookingStatus.REJECTED);
        rejectedBooking.setStart(LocalDateTime.now().plusHours(6));
        rejectedBooking.setEnd(LocalDateTime.now().plusHours(8));

        List<Booking> rejectedBookings = Collections.singletonList(rejectedBooking);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findByBookerIdAndStatus(booker.getId(), BookingStatus.REJECTED))
                .thenReturn(rejectedBookings);

        // When
        var result = new ArrayList<>(bookingService.getBookingsByState(booker.getId(), "REJECTED"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(bookingRepository, times(1))
                .findByBookerIdAndStatus(booker.getId(), BookingStatus.REJECTED);
    }

    @Test
    void create_ShouldThrowException_WhenItemNotFound() {
        // Given
        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(999L);
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(itemService.getByIdOrThrowInternal(999L)).thenThrow(NoSuchElementException.class);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, booker.getId()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_ShouldThrowException_WhenUserNotFound() {
        // Given
        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(item.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());

        when(userService.getByIdOrThrowInternal(booker.getId()))
                .thenThrow(NoSuchElementException.class);

        // When & Then
        assertThatThrownBy(() -> bookingService.create(dto, booker.getId()))
                .isInstanceOf(NoSuchElementException.class);
    }


    @Test
    void approveOrRejectBooking_ShouldThrowException_WhenBookingNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(bookingRepository.findByIdWithAssociations(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                bookingService.approveOrRejectBooking(nonExistentId, true, owner.getId()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Бронь с Id: " + nonExistentId + " не найдена");
    }

    @Test
    void getByIdForBookerOrOwner_ShouldThrowException_WhenBookingNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(bookingRepository.findByIdWithAssociations(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                bookingService.getByIdForBookerOrOwner(nonExistentId, booker.getId()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Бронь с Id: " + nonExistentId + " не существует");
    }

    @Test
    void getBookingsByState_ShouldReturnEmptyList_WhenUserHasNoBookings() {
        // Given
        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findAllByBookerId(booker.getId())).thenReturn(Collections.emptyList());

        // When
        var result = bookingService.getBookingsByState(booker.getId(), "ALL");

        // Then
        assertThat(result).isEmpty();
        verify(bookingRepository, times(1)).findAllByBookerId(booker.getId());
    }

    @Test
    void getBookingsByOwnerState_ShouldReturnEmptyList_WhenOwnerHasNoBookings() {
        // Given
        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findAllByItemOwnerId(owner.getId())).thenReturn(Collections.emptyList());

        // When
        var result = bookingService.getBookingsByOwnerState(owner.getId(), "ALL");

        // Then
        assertThat(result).isEmpty();
        verify(bookingRepository, times(1)).findAllByItemOwnerId(owner.getId());
    }

    @Test
    void getBookingsByState_ShouldSortResultsDescending() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Booking futureBooking1 = new Booking();
        futureBooking1.setId(1L);
        futureBooking1.setStart(now.plusDays(1));
        futureBooking1.setEnd(now.plusDays(2));

        Booking futureBooking2 = new Booking();
        futureBooking2.setId(2L);
        futureBooking2.setStart(now.plusDays(3));
        futureBooking2.setEnd(now.plusDays(4));

        List<Booking> bookings = Arrays.asList(futureBooking1, futureBooking2);

        when(userService.getByIdOrThrowInternal(booker.getId())).thenReturn(booker);
        when(bookingRepository.findFutureBookings(eq(booker.getId()), any(LocalDateTime.class)))
                .thenReturn(bookings);

        // When
        var result = new ArrayList<>(bookingService.getBookingsByState(booker.getId(), "FUTURE"));

        // Then: ожидаем обратный порядок (сначала более поздние бронирования)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(futureBooking2.getId());
        assertThat(result.get(1).getId()).isEqualTo(futureBooking1.getId());
    }

    @Test
    void getBookingsByOwnerState_ShouldSortResultsDescending() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Booking pastBooking1 = new Booking();
        pastBooking1.setId(1L);
        pastBooking1.setStart(now.minusDays(3));
        pastBooking1.setEnd(now.minusDays(2));

        Booking pastBooking2 = new Booking();
        pastBooking2.setId(2L);
        pastBooking2.setStart(now.minusDays(5));
        pastBooking2.setEnd(now.minusDays(4));


        List<Booking> bookings = Arrays.asList(pastBooking1, pastBooking2);

        when(userService.getByIdOrThrowInternal(owner.getId())).thenReturn(owner);
        when(bookingRepository.findPastBookingsForOwner(eq(owner.getId()), any(LocalDateTime.class)))
                .thenReturn(bookings);

        // When
        var result = new ArrayList<>(bookingService.getBookingsByOwnerState(owner.getId(), "PAST"));

        // Then: ожидаем обратный порядок
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(pastBooking1.getId());
        assertThat(result.get(1).getId()).isEqualTo(pastBooking2.getId());
    }

    @Test
    void findByIdOrThrowInternal_ShouldReturnBooking_WhenExists() {
        // Given
        when(bookingRepository.findByIdWithAssociations(booking.getId()))
                .thenReturn(Optional.of(booking));

        // When
        Booking result = bookingService.findByIdOrThrowInternal(booking.getId());

        // Then
        assertThat(result).isEqualTo(booking);
    }

}
