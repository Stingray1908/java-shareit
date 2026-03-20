package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.repository.InMemoryBookingRepository;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.repository.InMemoryItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.InMemoryUserRepository;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {
    @Autowired
    private InMemoryBookingRepository bookingRepository;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private InMemoryItemRepository itemRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingMapper bookingMapper;

    private final LocalDateTime march01 = LocalDateTime.of(2028, Month.MARCH, 1, 1, 0, 0);
    private final LocalDateTime march02 = LocalDateTime.of(2028, Month.MARCH, 2, 1, 0, 0);
    private final LocalDateTime march03 = LocalDateTime.of(2028, Month.MARCH, 3, 1, 0, 0);
    private final LocalDateTime march04 = LocalDateTime.of(2028, Month.MARCH, 4, 1, 0, 0);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private User owner;
    private User booker;
    private Item item;

    private String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // Заполняю тестовую среду:
    // создаю Owner и Booker
    // создаю вещь для Owner

    @BeforeEach
    public void setUp() {
        userRepository.clear();
        itemRepository.clear();
        bookingRepository.clear();

        owner = userRepository.save(new User(null, "Owner", "owner@example.com"));
        booker = userRepository.save(new User(null, "Booker", "booker@example.com"));

        item = itemRepository.create(new Item(
                null, owner.getId(), "ownerItem", "This item for test", null, true)
        );
    }

    private BookingReqDto getBookingReqDto(Long itemId, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        return bookingMapper.toReqDto(new Booking(null, null, itemId, start, end, status));
    }

    private ResultActions sendAndGetResponse(BookingReqDto dto, Long bookerId) throws Exception {
        if (bookerId == null) {
            throw new IllegalArgumentException("Booker ID cannot be null");
        }
        String reqDto = asJsonString(dto);

        System.out.println("Sending request with X-Booker-User-Id: " + bookerId);
        System.out.println("Request body: " + reqDto);

        return mockMvc.perform(post("/bookings")
                .header("X-Booker-User-Id", bookerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqDto));
    }


    @Test
    public void testBookingItem_Success() throws Exception {
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        sendAndGetResponse(reqDto, booker.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.bookerId", is(booker.getId().intValue()))) // Сравниваем с реальным ID
                .andExpect(jsonPath("$.itemId", is(item.getId().intValue())))
                .andExpect(jsonPath("$.start", is(march01.format(FORMATTER))))
                .andExpect(jsonPath("$.end", is(march02.format(FORMATTER))))
                .andExpect(jsonPath("$.status", is("WAITING")));
    }

    @Test
    public void testBookingItem_UserNotFound() throws Exception {
        // Дано: несуществующий ID пользователя
        Long nonExistentUserId = 999L;
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);

        // Когда: отправляем запрос с несуществующим пользователем
        sendAndGetResponse(reqDto, nonExistentUserId)
                // Тогда: ожидаем ошибку 404 Not Found
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Пользователь с ID 999 не найден")));
    }

    @Test
    public void testBookingItem_ItemNotFound() throws Exception {
        // Дано: несуществующий ID вещи
        Long nonExistentItemId = 999L;
        BookingReqDto reqDto = getBookingReqDto(nonExistentItemId, march01, march02, null);

        // Когда: отправляем запрос с несуществующей вещью
        sendAndGetResponse(reqDto, booker.getId())
                // Тогда: ожидаем ошибку 404 Not Found
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Вещь с ID: 999 не найдена")));
    }

    @Test
    public void testBookingItem_StartDateNotProvided() throws Exception {
        // Дано: DTO без времени начала
        BookingReqDto reqDto = getBookingReqDto(item.getId(), null, march02, null);
        // start не установлен

        // Когда: отправляем запрос без времени начала
        sendAndGetResponse(reqDto, booker.getId())
                // Тогда: ожидаем ошибку 400 Bad Request
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testBookingItem_StartAfterEnd() throws Exception {
        // Дано: время начала позже времени конца
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march02, march01, null);

        // Когда: отправляем запрос, где start > end
        sendAndGetResponse(reqDto, booker.getId())
                // Тогда: ожидаем ошибку 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Время начала не может быть после старта")));
    }

    @Test
    public void testBookingItem_CannotBookSameItemTwiceWithActiveStatus() throws Exception {
        // Дано: создаём первую бронь со статусом WAITING
        BookingReqDto firstBookingDto = getBookingReqDto(
                item.getId(),
                march01,
                march02,
                null
        );

        // Когда: отправляем запрос на создание первой брони
        sendAndGetResponse(firstBookingDto, booker.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("WAITING")));

        // И: пытаемся создать вторую бронь на тот же item от того же пользователя
        BookingReqDto secondBookingDto = getBookingReqDto(
                item.getId(),
                march03,
                march04,
                null
        );

        // Когда: отправляем запрос на создание второй брони
        MvcResult result = sendAndGetResponse(secondBookingDto, booker.getId())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Second booking response: " + responseBody);

        sendAndGetResponse(secondBookingDto, booker.getId())
                // Тогда: ожидаем ошибку 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(
                        "Бронирование невозможно: вещь с ID 1 уже имеет активный статус"
                )));
    }


    @Test
    public void testBookingItem_StartAndEndTimeAreEqual() throws Exception {
        // Дано: время начала и конца совпадают

        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march01, null);

        // Когда: отправляем запрос с одинаковыми временем начала и конца
        sendAndGetResponse(reqDto, booker.getId())
                // Тогда: ожидаем ошибку 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Время начала окончания брони не может совпадать")));
    }

    @Test
    public void testBookingItem_TimeOverlapWithExistingBooking() throws Exception {
        // Дано: создаём первую бронь
        BookingReqDto firstBookingDto = getBookingReqDto(
                item.getId(),
                march02,
                march03,
                null
        );

        // Когда: создаём первую бронь (она должна успешно создаться)
        sendAndGetResponse(firstBookingDto, booker.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("WAITING")));

        // И: пытаемся создать вторую бронь, которая пересекается с первой
        BookingReqDto overlappingBookingDto = getBookingReqDto(
                item.getId(),
                march01,
                march02,
                null
        );

        // Когда: отправляем запрос на создание пересекающейся брони
        sendAndGetResponse(overlappingBookingDto, booker.getId()) // Используем booker вместо another
                // Тогда: ожидаем ошибку 400 Bad Request из‑за пересечения
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Бронирование невозможно: вещь с ID 1 уже имеет активный статус")));
    }


    @Test
    public void testBookingItem_CannotBookOwnItem() throws Exception {
        // Дано: владелец вещи пытается забронировать свою же вещь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);

        // Когда: отправляем запрос на бронирование с ID владельца (owner.getId())
        sendAndGetResponse(reqDto, owner.getId())
                // Тогда: ожидаем ошибку 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(
                        "Владелец не может бронировать свои же вещи"
                )));
    }

    @Test
    public void testPatchBooking_OwnerApproveSuccess() throws Exception {
        // Дано: создаём бронь со статусом WAITING
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: владелец (owner) утверждает бронь (статус APPROVED)
        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setStatus(BookingStatus.APPROVED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchDto)))
                // Тогда: статус успешно изменён
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    public void testPatchBooking_BookerCancelSuccess() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: booker отменяет бронь
        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setStatus(BookingStatus.CANCELED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchDto)))
                // Тогда: статус изменён на CANCELED
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELED")));
    }

    @Test
    public void testPatchBooking_UnauthorizedUser() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: пользователь, не являющийся ни booker, ни owner, пытается обновить статус
        User unauthorizedUser = userRepository.save(new User(null, "Unauthorized", "unauth@example.com"));

        BookingReqDto patchDto = new BookingReqDto();
        patchDto.setStatus(BookingStatus.CANCELED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", unauthorizedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchDto)))
                // Тогда: ошибка 403 Forbidden
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPatchBooking_CannotUpdateCancelledBooking() throws Exception {
        // Дано: бронь, уже отменённая booker
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Сначала отменяем бронь
        BookingReqDto cancelDto = new BookingReqDto();
        cancelDto.setStatus(BookingStatus.CANCELED);
        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(cancelDto)))
                .andExpect(status().isOk());

        // Когда: пытаемся изменить статус у уже отменённой брони
        BookingReqDto newStatusDto = new BookingReqDto();
        newStatusDto.setStatus(BookingStatus.APPROVED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(newStatusDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Нельзя изменить статус завершённой брони")));
    }

    @Test
    public void testPatchBooking_OnlyStatusCanBeUpdated() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: пытаемся обновить start или itemId
        BookingReqDto invalidPatchDto = new BookingReqDto();
        invalidPatchDto.setStart(march03);
        invalidPatchDto.setItemId(999L);
        invalidPatchDto.setStatus(BookingStatus.APPROVED); // статус меняем

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidPatchDto)))
                // Тогда: ошибка валидации
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetBooking_BookerSuccess() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: booker запрашивает свою бронь
        mockMvc.perform(get("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", booker.getId()))
                // Тогда: бронь возвращается
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdBooking.getId().intValue())));
    }

    @Test
    public void testGetBooking_OwnerSuccess() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        mockMvc.perform(get("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdBooking.getId().intValue())));
    }

    @Test
    public void testGetBooking_UnauthorizedUser() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: пользователь, не являющийся ни booker, ни owner, пытается получить бронь
        User unauthorizedUser = userRepository.save(new User(null, "Unauthorized", "unauth@example.com"));

        mockMvc.perform(get("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", unauthorizedUser.getId()))
                // Тогда: ошибка 403 Forbidden
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Пользователь с ID: 3 не имеет доступа к брони с ID: 1")));
    }

    @Test
    public void testGetBooking_NotFound() throws Exception {
        // Дано: несуществующий ID брони
        Long nonExistentBookingId = 999L;

        // Когда: запрашиваем несуществующую бронь
        mockMvc.perform(get("/bookings/" + nonExistentBookingId)
                        .header("X-User-Id", booker.getId()))
                // Тогда: ошибка 404 Not Found
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Нет брони с ID: 999 для пользователя 2")));
    }

    @Test
    public void testDeleteBooking_BookerSuccess() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: booker удаляет свою бронь
        mockMvc.perform(delete("/bookings/" + createdBooking.getId())
                        .header("X-Booker-User-Id", booker.getId()))
                // Тогда: статус 200 OK, бронь удалена
                .andExpect(status().isOk());

        // Проверяем, что бронь действительно удалена
        mockMvc.perform(get("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", booker.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteBooking_NotBooker() throws Exception {
        // Дано: существующая бронь
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Когда: владелец вещи (owner) пытается удалить бронь
        mockMvc.perform(delete("/bookings/" + createdBooking.getId())
                        .header("X-Booker-User-Id", owner.getId()))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Только создатель брони (Booker) может её удалить")));
    }

    @Test
    public void testGetUserBookings_UserHasBookings() throws Exception {
        // Дано: создаём несколько броней для booker
        Item item1 = itemRepository.create(new Item(null, 1L, "Бигуди", "Заваивают", null, true));
        BookingReqDto booking1 = getBookingReqDto(item.getId(), march01, march02, null);
        BookingReqDto booking2 = getBookingReqDto(item1.getId(), march03, march04, null);

        // Создаём первую бронь
        sendAndGetResponse(booking1, booker.getId())
                .andExpect(status().isCreated());

        // Создаём вторую бронь
        sendAndGetResponse(booking2, booker.getId())
                .andExpect(status().isCreated());

        // Когда: запрашиваем все брони пользователя
        MvcResult result = mockMvc.perform(get("/bookings")
                        .header("X-User-Id", booker.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // Тогда: получаем список из двух броней
        String responseBody = result.getResponse().getContentAsString();
        List<BookingSendDto> bookings = objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, BookingSendDto.class)
        );

        assertThat(bookings).hasSize(2);
        assertThat(bookings)
                .extracting("bookerId")
                .containsOnly(booker.getId());
    }

    @Test
    public void testGetUserBookings_UserHasNoBookings() throws Exception {
        // Дано: пользователь существует, но у него нет броней
        Long existingUserId = booker.getId();

        // Когда: запрашиваем брони пользователя без броней
        MvcResult result = mockMvc.perform(get("/bookings")
                        .header("X-User-Id", existingUserId))
                .andExpect(status().isOk())
                .andReturn();

        // Тогда: получаем пустой список
        String responseBody = result.getResponse().getContentAsString();
        List<BookingSendDto> bookings = objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, BookingSendDto.class)
        );

        assertThat(bookings).isEmpty();
    }

    @Test
    public void testOwnerSetCompletedStatus_Success() throws Exception {
        // Дано: существующая бронь со статусом APPROVED
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Сначала owner утверждает бронь
        BookingReqDto approveDto = new BookingReqDto();
        approveDto.setStatus(BookingStatus.APPROVED);
        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Когда: owner устанавливает статус COMPLETED
        BookingReqDto completeDto = new BookingReqDto();
        completeDto.setStatus(BookingStatus.COMPLETED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(completeDto)))
                // Тогда: статус успешно изменён на COMPLETED
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    public void testCannotUpdateCompletedBooking_Error() throws Exception {
        // Дано: бронь, уже завершённая (COMPLETED)
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Owner утверждает бронь
        BookingReqDto approveDto = new BookingReqDto();
        approveDto.setStatus(BookingStatus.APPROVED);
        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(approveDto)))
                .andExpect(status().isOk());

        // Owner завершает бронь
        BookingReqDto completeDto = new BookingReqDto();
        completeDto.setStatus(BookingStatus.COMPLETED);
        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(completeDto)))
                .andExpect(status().isOk());

        // Когда: пытаемся изменить статус у уже завершённой брони
        BookingReqDto newStatusDto = new BookingReqDto();
        newStatusDto.setStatus(BookingStatus.APPROVED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(newStatusDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Нельзя изменить статус завершённой брони")));
    }

    @Test
    public void testBookerCannotSetCompletedStatus_Error() throws Exception {
        // Дано: существующая бронь со статусом WAITING
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        BookingSendDto.class
                );

        // Когда: booker пытается установить статус COMPLETED (что запрещено)
        BookingReqDto completeDto = new BookingReqDto();
        completeDto.setStatus(BookingStatus.COMPLETED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(completeDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Booker может только отменить бронь (статус CANCELED)")));
    }

    @Test
    public void testNewBookingWhenCompletedExists_Success() throws Exception {
        // Дано: существующая завершённая бронь (COMPLETED) для того же пользователя
        BookingReqDto completedBookingDto = getBookingReqDto(
                item.getId(),
                march01,
                march02,
                null
        );
        BookingSendDto completedBooking = objectMapper.readValue(
                (sendAndGetResponse(completedBookingDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()),
                BookingSendDto.class
        );

        // Owner утверждает и завершает первую бронь
        BookingReqDto approveDto = new BookingReqDto();
        approveDto.setStatus(BookingStatus.APPROVED);
        mockMvc.perform(patch("/bookings/" + completedBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(approveDto)))
                .andExpect(status().isOk());

        BookingReqDto completeDto = new BookingReqDto();
        completeDto.setStatus(BookingStatus.COMPLETED);
        mockMvc.perform(patch("/bookings/" + completedBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(completeDto)))
                .andExpect(status().isOk());

        // И: пытаемся создать новую бронь на тот же item от того же пользователя
        BookingReqDto newBookingDto = getBookingReqDto(
                item.getId(),
                march03,
                march04,
                null
        );

        // Когда: отправляем запрос на создание новой брони
        sendAndGetResponse(newBookingDto, booker.getId())
                // Тогда: новая бронь создаётся успешно (COMPLETED не блокирует)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("WAITING")));
    }

    @Test
    public void testOwnerChangeApprovedToCompleted_Success() throws Exception {
        // Дано: существующая бронь со статусом APPROVED
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Owner утверждает бронь
        BookingReqDto approveDto = new BookingReqDto();
        approveDto.setStatus(BookingStatus.APPROVED);
        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Когда: owner меняет статус с APPROVED на COMPLETED
        BookingReqDto completeDto = new BookingReqDto();
        completeDto.setStatus(BookingStatus.COMPLETED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(completeDto)))
                // Тогда: статус успешно изменён на COMPLETED
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    public void testOwnerCannotChangeApprovedToRejected_Error() throws Exception {
        // Дано: существующая бронь со статусом APPROVED
        BookingReqDto reqDto = getBookingReqDto(item.getId(), march01, march02, null);
        BookingSendDto createdBooking = objectMapper.readValue(
                sendAndGetResponse(reqDto, booker.getId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                BookingSendDto.class
        );

        // Owner утверждает бронь
        BookingReqDto approveDto = new BookingReqDto();
        approveDto.setStatus(BookingStatus.APPROVED);
        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Когда: owner пытается изменить статус с APPROVED на REJECTED (что запрещено после одобрения)
        BookingReqDto rejectDto = new BookingReqDto();
        rejectDto.setStatus(BookingStatus.REJECTED);

        mockMvc.perform(patch("/bookings/" + createdBooking.getId())
                        .header("X-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(rejectDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Owner не может изменить статус 'подтверждено' на 'REJECTED'. Разрешён только 'завершено'")));
    }
}


