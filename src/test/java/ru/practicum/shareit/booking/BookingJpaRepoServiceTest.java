package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.TestServiceConfiguration;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.booking.service.BookingJpaService;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.item.service.ItemJPAService;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestServiceConfiguration.class)
public class BookingJpaRepoServiceTest {

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
        return LocalDateTime.now().plusHours(hours);
    }

    private BookingReqDto createBookingDto(Long itemId, Long bookerId, int startHours, int endHours) {
        LocalDateTime start = getFutureTime(startHours);
        LocalDateTime end = getFutureTime(endHours);

        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(itemId);
        dto.setBookerId(bookerId);
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

    // Тесты создания брони
    @Test
    void create_shouldCreateBookingSuccessfully() {
        BookingReqDto dto = createBookingDto(itemId, bookerId, 1, 2);
        BookingSendDto result = bookingService.create(dto);

        assertThat(result)
                .hasFieldOrPropertyWithValue("status", BookingStatus.WAITING)
                .hasFieldOrPropertyWithValue("booker.id", bookerId)
                .hasFieldOrPropertyWithValue("item.id", itemId);
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void create_shouldThrowException_WhenUserTriesToBookOwnItem() {
        BookingReqDto dto = createBookingDto(itemId, ownerId, 1, 2);

        assertThatThrownBy(() -> bookingService.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь не может бронировать свои вещи");
    }

    @Test
    void create_shouldThrowException_WhenItemIsNotAvailable() {
        makeItemUnavailable(itemId);
        BookingReqDto dto = createBookingDto(itemId, bookerId, 1, 2);

        assertThatThrownBy(() -> bookingService.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Вещь недоступна (available = false)");
    }

    @Test
    void create_shouldThrowException_WhenBookingTimeOverlaps() {
        // Создаём существующую бронь
        bookingService.create(createBookingDto(itemId, bookerId, 1, 3));

        // Пытаемся создать пересекающуюся
        BookingReqDto newDto = createBookingDto(itemId, anotherBookerId, 2, 4);

        assertThatThrownBy(() -> bookingService.create(newDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("время уже забронировано");
    }

    @Test
    void create_shouldThrowException_WhenStartAfterEnd() {
        BookingReqDto dto = createBookingDto(itemId, bookerId, 2, 1);

        assertThatThrownBy(() -> bookingService.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Время начала не может быть после старта");
    }

    @Test
    void create_shouldThrowException_WhenStartEqualsEnd() {
        LocalDateTime time = getFutureTime(1);
        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(itemId);
        dto.setBookerId(bookerId);
        dto.setStart(time);
        dto.setEnd(time);

        assertThatThrownBy(() -> bookingService.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Время начала окончания брони не может совпадать");
    }

    // Тесты обновления статуса
    private BookingSendDto createAndApproveBooking() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));
        BookingReqDto approveDto = new BookingReqDto();
        approveDto.setId(created.getId());
        approveDto.setStatus(BookingStatus.APPROVED);
        return bookingService.patchBooking(approveDto, ownerId);
    }

    @Test
    void patchBooking_shouldUpdateStatusSuccessfully_WhenOwnerApproves() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.APPROVED);

        BookingSendDto updated = bookingService.patchBooking(patchDto, ownerId);

        assertThat(updated).hasFieldOrPropertyWithValue("status", BookingStatus.APPROVED);
    }

    @Test
    void patchBooking_shouldUpdateStatusSuccessfully_WhenBookerCancels() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.CANCELLED);

        BookingSendDto updated = bookingService.patchBooking(patchDto, bookerId);

        assertThat(updated).hasFieldOrPropertyWithValue("status", BookingStatus.CANCELLED);
    }

    @Test
    void patchBooking_shouldReturnSameBooking_WhenStatusNotChanged() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.WAITING);

        BookingSendDto result = bookingService.patchBooking(patchDto, ownerId);

        assertThat(result).hasFieldOrPropertyWithValue("status", BookingStatus.WAITING);
    }

    @Test
    void patchBooking_shouldThrowException_WhenBookingNotFound() {
        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(999L);
        patchDto.setStatus(BookingStatus.APPROVED);

        assertThatThrownBy(() -> bookingService.patchBooking(patchDto, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Бронь с Id: 999 с активным статусом не найдена");
    }

    @Test
    void patchBooking_shouldThrowException_WhenUserHasNoPermissions() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.APPROVED);

        assertThatThrownBy(() -> bookingService.patchBooking(patchDto, anotherBookerId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("не имеет прав на изменение брони");
    }

    @Test
    void patchBooking_shouldThrowException_WhenBookerTriesToChangeToNonCancelledStatus() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.APPROVED);

        assertThatThrownBy(() -> bookingService.patchBooking(patchDto, bookerId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("может установить только статус CANCELLED");
    }

    @Test
    void patchBooking_shouldThrowException_WhenOwnerTriesToSetCancelled() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.CANCELLED);

        String expectedMessage = String.format(
                ("Владелец вещи Id: %d не может установить статус CANCELLED для брони Id: %d"), ownerId, created.getId());

        assertThatThrownBy(() -> bookingService.patchBooking(patchDto, ownerId))
                .isInstanceOf(SecurityException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void patchBooking_shouldThrowException_WhenTransitionIsInvalid() {
        BookingSendDto created = createAndApproveBooking();

        BookingReqDto invalidPatchDto = new BookingReqDto();
        invalidPatchDto.setId(created.getId());
        invalidPatchDto.setStatus(BookingStatus.WAITING);

        String expectedMessage = String.format(
        ("Владелец вещи Id: %d не может установить статус WAITING для брони Id: %d"), ownerId, created.getId());

        assertThatThrownBy(() -> bookingService.patchBooking(invalidPatchDto, ownerId))
                .isInstanceOf(SecurityException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void patchBooking_shouldThrowException_WhenChangingFinalStatus() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        // Отменяем бронь — устанавливаем финальный статус
        BookingReqDto cancelDto = new BookingReqDto();
        cancelDto.setId(created.getId());
        cancelDto.setStatus(BookingStatus.CANCELLED);
        bookingService.patchBooking(cancelDto, bookerId);

        // Пытаемся изменить финальный статус на APPROVED
        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.APPROVED);

        assertThatThrownBy(() -> bookingService.patchBooking(patchDto, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Бронь с Id: " + created.getId() + " с активным статусом не найдена");
    }

    @Test
    void patchBooking_shouldAllowBookerToCancelApprovedBooking() {
        BookingSendDto approved = createAndApproveBooking();

        BookingReqDto cancelDto = new BookingReqDto();
        cancelDto.setId(approved.getId());
        cancelDto.setStatus(BookingStatus.CANCELLED);

        BookingSendDto updated = bookingService.patchBooking(cancelDto, bookerId);

        assertThat(updated).hasFieldOrPropertyWithValue("status", BookingStatus.CANCELLED);
    }

    @Test
    void patchBooking_shouldAllowOwnerToSetCompletedFromApproved() {
        BookingSendDto approved = createAndApproveBooking();

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(approved.getId());
        patchDto.setStatus(BookingStatus.COMPLETED);

        BookingSendDto updated = bookingService.patchBooking(patchDto, ownerId);

        assertThat(updated).hasFieldOrPropertyWithValue("status", BookingStatus.COMPLETED);
    }

    @Test
    void patchBooking_shouldAllowStatusUpdate() {
        BookingSendDto created = bookingService.create(createBookingDto(itemId, bookerId, 1, 2));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setId(created.getId());
        patchDto.setStatus(BookingStatus.REJECTED);

        assertDoesNotThrow(() -> bookingService.patchBooking(patchDto, ownerId));
    }
}

