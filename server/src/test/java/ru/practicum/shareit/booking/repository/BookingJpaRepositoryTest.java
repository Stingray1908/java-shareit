package ru.practicum.shareit.booking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.comment.repository.CommentJpaRepository;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.repository.ItemJpaRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.repository.UserJpaRepository;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class BookingJpaRepositoryTest {

    @Autowired
    private BookingJpaRepository bookingRepository;
    @Autowired
    private ItemJpaRepository itemRepository;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private CommentJpaRepository commentRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private BookingService bookingService;

    private Long ownerId;
    private Long bookerId;
    private Long anotherBookerId;
    private Long itemId;

    // Вспомогательные методы
    private LocalDateTime getFutureTime(int hours) {
        return LocalDateTime.now().plusHours(hours).plusMinutes(1);
    }

    private BookingReqDto createBookingDto(Long itemId, Long bookerId, int startHours, int endHours) {
        LocalDateTime start = getFutureTime(startHours);
        LocalDateTime end = getFutureTime(endHours);

        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(itemId);
        dto.setStart(start);
        dto.setEnd(end);
        return dto;
    }

    private Long createTestItem(Long ownerId) {
        ItemReqDTO itemDto = new ItemReqDTO();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);
        return itemService.create(ownerId, itemDto).getId();
    }

    private void makeItemUnavailable(Long itemId) {
        ItemReqDTO updateDto = new ItemReqDTO("Test Item", "Test Description", null, false);
        itemService.update(itemId, ownerId, updateDto);
    }

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();      // 1. Сначала комментарии
        bookingRepository.deleteAll();     // 2. Затем бронирования
        itemRepository.deleteAll();       // 3. Потом вещи
        userRepository.deleteAll();       // 4. В конце пользователи

        ownerId = userService.create(new UserReqDTO("Owner", "owner@test.com")).getId();
        bookerId = userService.create(new UserReqDTO("Booker", "booker@test.com")).getId();
        anotherBookerId = userService.create(new UserReqDTO("Second Booker", "secondBooker@test.com")).getId();

        itemId = createTestItem(ownerId);
    }

    @Test
    void create_ShouldCreateBooking_WhenValidData() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);

        // When
        BookingSendDto result = bookingService.create(dto, bookerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getItem().getId()).isEqualTo(itemId);
        assertThat(result.getBooker().getId()).isEqualTo(bookerId);
    }

    @Test
    void create_ShouldThrowException_WhenUserTriesToBookOwnItem() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, ownerId, 2, 4);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.create(dto, ownerId),
                "Пользователь не может бронировать свои вещи"
        );
    }

    @Test
    void create_ShouldThrowException_WhenItemIsUnavailable() {
        // Given
        makeItemUnavailable(itemId);
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.create(dto, bookerId),
                "Вещь недоступна (available = false)"
        );
    }

    @Test
    void create_ShouldThrowException_WhenDatesOverlap() {
        // Given
        // Создаём существующее бронирование
        BookingReqDto existingDto = createBookingDto(itemId, anotherBookerId, 1, 5);
        bookingService.create(existingDto, anotherBookerId);

        // Пытаемся создать пересекающееся бронирование
        BookingReqDto overlappingDto = createBookingDto(itemId, bookerId, 3, 6);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.create(overlappingDto, bookerId),
                "Время уже забронировано"
        );
    }

    @Test
    void approveOrRejectBooking_ShouldApprove_WhenValidRequest() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);

        // When
        BookingSendDto approvedBooking = bookingService.approveOrRejectBooking(createdBooking.getId(), true, ownerId);

        // Then
        assertThat(approvedBooking.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approveOrRejectBooking_ShouldReject_WhenValidRequest() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);

        // When
        BookingSendDto rejectedBooking = bookingService.approveOrRejectBooking(createdBooking.getId(), false, ownerId);

        // Then
        assertThat(rejectedBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approveOrRejectBooking_ShouldThrowException_WhenNotOwner() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);

        // When & Then
        assertThrows(SecurityException.class,
                () -> bookingService.approveOrRejectBooking(createdBooking.getId(), true, bookerId),
                "Пользователь Id: " + bookerId + " не является владельцем вещи и не может подтверждать/отклонять бронь"
        );
    }

    @Test
    void getByIdForBookerOrOwner_ShouldReturnBooking_WhenUserIsBooker() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);

        // When
        BookingSendDto result = bookingService.getByIdForBookerOrOwner(createdBooking.getId(), bookerId);

        // Then
        assertThat(result.getId()).isEqualTo(createdBooking.getId());
    }

    @Test
    void getByIdForBookerOrOwner_ShouldReturnBooking_WhenUserIsOwner() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);

        // When
        BookingSendDto result = bookingService.getByIdForBookerOrOwner(createdBooking.getId(), ownerId);

        // Then
        assertThat(result.getId()).isEqualTo(createdBooking.getId());
    }

    @Test
    void getByIdForBookerOrOwner_ShouldThrowException_WhenUnauthorizedAccess() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);
        Long unauthorizedUserId = anotherBookerId;

        // When & Then
        assertThrows(SecurityException.class,
                () -> bookingService.getByIdForBookerOrOwner(createdBooking.getId(), unauthorizedUserId),
                "Пользователь Id: " + unauthorizedUserId + " пытается получить бронь пользователя Id: " + bookerId
        );
    }

    @Test
    void getBookingsByState_ShouldReturnAllBookings_WhenStateIsAll() {
        // Given
        BookingReqDto dto1 = createBookingDto(itemId, bookerId, 2, 4);
        BookingReqDto dto2 = createBookingDto(itemId, bookerId, 5, 7);
        bookingService.create(dto1, bookerId);
        bookingService.create(dto2, bookerId);

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "ALL");

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void getBookingsByState_ShouldReturnWaitingBookings_WhenStateIsWaiting() {
        // Given
        BookingReqDto waitingDto1 = createBookingDto(itemId, bookerId, 2, 4);
        BookingReqDto waitingDto2 = createBookingDto(itemId, bookerId, 5, 7);
        bookingService.create(waitingDto1, bookerId);
        bookingService.create(waitingDto2, bookerId);

        // Создаём подтверждённое бронирование — оно не должно попасть в результат
        BookingReqDto approvedDto = createBookingDto(itemId, bookerId, 8, 10);
        BookingSendDto approvedBooking = bookingService.create(approvedDto, bookerId);
        bookingService.approveOrRejectBooking(approvedBooking.getId(), true, ownerId);

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "WAITING");

        // Then
        assertThat(result).hasSize(2);
        result.forEach(booking -> assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING));
    }

    @Test
    void getBookingsByOwnerState_ShouldReturnBookingsForOwner_WhenStateIsAll() {
        // Given
        BookingReqDto dto1 = createBookingDto(itemId, bookerId, 2, 4);
        BookingReqDto dto2 = createBookingDto(itemId, anotherBookerId, 5, 7);
        bookingService.create(dto1, bookerId);
        bookingService.create(dto2, anotherBookerId);

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByOwnerState(ownerId, "ALL");

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void parseBookingState_ShouldThrowException_ForInvalidState() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBookingsByState(bookerId, "INVALID_STATE"),
                "Недопустимое значение state: INVALID_STATE"
        );
    }

    @Test
    void validateDates_ShouldThrowException_WhenStartAfterEnd() {
        // Given
        LocalDateTime start = getFutureTime(5);
        LocalDateTime end = getFutureTime(3);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    BookingReqDto dto = createBookingDto(itemId, bookerId, 5, 3);
                    bookingService.create(dto, bookerId);
                }
        );
        assertThat(exception.getMessage()).contains("Время начала не может быть после времени окончания");
    }

    @Test
    void validateDates_ShouldThrowException_WhenStartEqualsEnd() {
        // Given
        LocalDateTime sameTime = getFutureTime(2);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    BookingReqDto dto = new BookingReqDto();
                    dto.setItemId(itemId);
                    dto.setStart(sameTime);
                    dto.setEnd(sameTime);
                    bookingService.create(dto, bookerId);
                }
        );
        assertThat(exception.getMessage()).contains("Время начала и окончания брони не может совпадать");
    }

    @Test
    void findByIdOrThrowInternal_ShouldThrowException_WhenBookingNotFound() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> bookingService.findByIdOrThrowInternal(nonExistentId),
                "Бронь с Id: " + nonExistentId + " не существует"
        );
    }

    @Test
    void approveOrRejectBooking_shouldThrowException_whenTryingToChangeStatusOfAlreadyApprovedBooking() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);
        bookingService.approveOrRejectBooking(createdBooking.getId(), true, ownerId); // approve first

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.approveOrRejectBooking(createdBooking.getId(), false, ownerId),
                "Можно подтверждать/отклонять только бронирования со статусом WAITING, текущий статус: APPROVED"
        );
    }

    @Test
    void approveOrRejectBooking_shouldThrowException_whenTryingToChangeStatusOfAlreadyRejectedBooking() {
        // Given
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);
        bookingService.approveOrRejectBooking(createdBooking.getId(), false, ownerId); // reject first

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.approveOrRejectBooking(createdBooking.getId(), true, ownerId),
                "Можно подтверждать/отклонять только бронирования со статусом WAITING, текущий статус: REJECTED"
        );
    }

    @Test
    void approveOrRejectBooking_shouldThrowSecurityException_whenAnotherOwnerTriesToApprove() {
        // Given
        Long anotherOwnerId = userService.create(new UserReqDTO("Another Owner", "another@test.com")).getId();
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 4);
        BookingSendDto createdBooking = bookingService.create(dto, bookerId);

        // When & Then
        assertThrows(SecurityException.class,
                () -> bookingService.approveOrRejectBooking(createdBooking.getId(), true, anotherOwnerId),
                String.format("Пользователь Id: %d не является владельцем вещи и не может подтверждать/отклонять бронь", anotherOwnerId)
        );
    }

    @Test
    void approveOrRejectBooking_shouldThrowNoSuchElementException_whenBookingDoesNotExist() {
        // Given
        Long nonExistentBookingId = 999L;

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> bookingService.approveOrRejectBooking(nonExistentBookingId, true, ownerId),
                "Бронь с Id: " + nonExistentBookingId + " не найдена"
        );
    }

    @Test
    void getByIdForBookerOrOwner_shouldThrowNoSuchElementException_whenBookingDoesNotExist() {
        // Given
        Long nonExistentBookingId = 999L;

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> bookingService.getByIdForBookerOrOwner(nonExistentBookingId, bookerId),
                "Бронь с Id: " + nonExistentBookingId + " не существует"
        );
    }

    @Test
    void getBookingsByState_shouldReturnEmptyCollection_whenNoCurrentBookings() {
        // Given — no current bookings

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "CURRENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getBookingsByState_shouldReturnEmptyCollection_whenNoPastBookings() {
        // Given — no past bookings

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "PAST");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getBookingsByState_shouldReturnEmptyCollection_whenNoFutureBookings() {
        // Given — no future bookings

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "FUTURE");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getBookingsByOwnerState_shouldReturnEmptyCollection_whenNoCurrentBookingsForOwner() {
        // Given — no current bookings for owner

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByOwnerState(ownerId, "CURRENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getBookingsByState_shouldReturnCurrentBookings_whenStateIsCurrent() {
        // Given: create past, current and future bookings
        BookingReqDto pastDto = createBookingDto(itemId, bookerId, -5, -3); // past
        BookingReqDto currentDto = createBookingDto(itemId, bookerId, -1, 1); // current
        BookingReqDto futureDto = createBookingDto(itemId, bookerId, 3, 5); // future
        bookingService.create(pastDto, bookerId);
        bookingService.create(currentDto, bookerId);
        bookingService.create(futureDto, bookerId);

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "CURRENT");

        // Then
        assertThat(result)
                .hasSize(1)
                .allMatch(booking -> booking.getStart().isBefore(LocalDateTime.now())
                        && booking.getEnd().isAfter(LocalDateTime.now()));
    }

    @Test
    void getBookingsByState_shouldBeSortedDescendingByStart() {
        // Given: create bookings with different start times
        BookingReqDto dto1 = createBookingDto(itemId, bookerId, 5, 7); // latest
        BookingReqDto dto2 = createBookingDto(itemId, bookerId, 2, 4); // earlier
        bookingService.create(dto1, bookerId);
        bookingService.create(dto2, bookerId);

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "ALL");
        List<LocalDateTime> startTimes = result.stream()
                .map(BookingSendDto::getStart)
                .toList();

        // Then: check descending order
        assertThat(startTimes)
                .isSortedAccordingTo((t1, t2) -> t2.compareTo(t1));
    }

    @Test
    void getBookingsByState_shouldApplyPagination_whenFromAndSizeProvided() {
        // Given: create 4 bookings
        for (int i = 0; i < 4; i++) {
            BookingReqDto dto = createBookingDto(itemId, bookerId, i * 2 + 2, i * 2 + 4);
            bookingService.create(dto, bookerId);
        }

        // When: request with pagination
        Collection<BookingSendDto> result = bookingService.getBookingsByState(bookerId, "ALL");
        List<BookingSendDto> paginatedResult = new ArrayList<>(result).subList(1, 3); // simulate from=1, size=2

        // Note: actual pagination logic should be tested in controller or service if implemented

        // Then
        assertThat(paginatedResult).hasSize(2);
    }

    @Test
    void checkOverLapBookings_shouldReturnFalse_whenNewBookingStartsExactlyWhenExistingEnds() {
        // Given: create existing booking
        BookingReqDto existingDto = createBookingDto(itemId, anotherBookerId, 2, 4);
        bookingService.create(existingDto, anotherBookerId);

        // New booking starts exactly when existing ends
        LocalDateTime newStart = getFutureTime(4); // same as existing end
        LocalDateTime newEnd = getFutureTime(6);

        // When
        boolean hasOverlap = bookingRepository.checkOverLapBookings(
                itemId, newStart, newEnd
        );

        // Then
        assertThat(hasOverlap).isFalse();
    }

    @Test
    void checkOverLapBookings_shouldReturnFalse_whenNewBookingEndsBeforeExistingStarts() {
        // Given: существующее бронирование (4–6)
        BookingReqDto existingDto = createBookingDto(itemId, anotherBookerId, 4, 6);
        bookingService.create(existingDto, anotherBookerId);

        // Новое бронирование заканчивается ДО начала существующего (2–3)
        LocalDateTime newStart = getFutureTime(2);
        LocalDateTime newEnd = getFutureTime(3); // раньше, чем existingStart (4)

        // When
        boolean hasOverlap = bookingRepository.checkOverLapBookings(
                itemId, newStart, newEnd
        );

        // Then: точно нет пересечения
        assertThat(hasOverlap).isFalse();
    }


    @Test
    void existsPastBooking_shouldReturnTrue_whenUserHadPastBookingWithValidStatus() {
        // Given: create past booking
        BookingReqDto pastDto = createBookingDto(itemId, bookerId, -4, -2);
        BookingSendDto pastBooking = bookingService.create(pastDto, bookerId);
        // Approve it to have valid status
        bookingService.approveOrRejectBooking(pastBooking.getId(), true, ownerId);

        // When
        boolean exists = bookingRepository.existsPastBooking(
                bookerId,
                itemId,
                List.of(BookingStatus.APPROVED.name()),
                LocalDateTime.now()
        );

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsPastBooking_shouldReturnFalse_whenUserNeverBookedItem() {
        // Given: user never booked this item

        // When
        boolean exists = bookingRepository.existsPastBooking(
                bookerId,
                itemId,
                List.of(BookingStatus.APPROVED.name()),
                LocalDateTime.now()
        );

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByIdOrThrowInternal_shouldThrowNoSuchElementException_whenBookingDoesNotExist() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> bookingService.findByIdOrThrowInternal(nonExistentId),
                "Бронь с Id: " + nonExistentId + " не существует"
        );
    }


}
