package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.booking.service.BookingJpaService;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.item.service.ItemJPAService;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingJpaRepoServiceTest {

    @Autowired
    private BookingJpaRepository bookingRepository;
    @Autowired
    private ItemJPARepository itemRepository;
    @Autowired
    private UserJPARepository userRepository;
    @Autowired
    private UserJPAService userService;
    @Autowired
    private ItemJPAService itemService;
    @Autowired
    private BookingJpaService bookingService;

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
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        itemRepository.deleteAll();

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
    void validateDates_ShouldThrowException_WhenStartInPast() {
        // Given
        LocalDateTime pastTime = LocalDateTime.now().minusHours(2);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    BookingReqDto dto = new BookingReqDto();
                    dto.setItemId(itemId);
                    dto.setStart(pastTime);
                    dto.setEnd(pastTime.plusHours(2));
                    bookingService.create(dto, bookerId);
                }
        );
        assertThat(exception.getMessage()).contains("Время начала должно быть в будущем");
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
    void getBookingsByOwnerState_ShouldReturnRejectedBookingsForOwner() {
        // Given
        BookingReqDto rejectedDto1 = createBookingDto(itemId, bookerId, 2, 4);
        BookingReqDto rejectedDto2 = createBookingDto(itemId, anotherBookerId, 5, 7);
        BookingSendDto booking1 = bookingService.create(rejectedDto1, bookerId);
        BookingSendDto booking2 = bookingService.create(rejectedDto2, anotherBookerId);

        // Отклоняем бронирования
        bookingService.approveOrRejectBooking(booking1.getId(), false, ownerId);
        bookingService.approveOrRejectBooking(booking2.getId(), false, ownerId);

        // Создаём подтверждённое бронирование — оно не должно попасть в результат
        BookingReqDto approvedDto = createBookingDto(itemId, bookerId, 8, 10);
        BookingSendDto approvedBooking = bookingService.create(approvedDto, bookerId);
        bookingService.approveOrRejectBooking(approvedBooking.getId(), true, ownerId);

        // When
        Collection<BookingSendDto> result = bookingService.getBookingsByOwnerState(ownerId, "REJECTED");

        // Then
        assertThat(result).hasSize(2);
        result.forEach(booking -> assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED));
    }
}
