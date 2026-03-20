package ru.practicum.shareit.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.repository.InMemoryBookingRepository;
import ru.practicum.shareit.comment.dto.CommentReqDto;
import ru.practicum.shareit.comment.dto.CommentSendDto;
import ru.practicum.shareit.comment.repository.InMemoryCommentRepository;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.repository.InMemoryItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.InMemoryUserRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private InMemoryItemRepository itemRepository;

    @Autowired
    private InMemoryBookingRepository bookingRepository;

    @Autowired
    private InMemoryCommentRepository commentRepository;

    private User owner;
    private User booker;
    private Item item;
    private Booking completedBooking;

    private String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @BeforeEach
    void setUp() {
        userRepository.clear();
        itemRepository.clear();
        bookingRepository.clear();
        commentRepository.clear();

        owner = userRepository.save(new User(null, "Owner", "owner@example.com"));
        booker = userRepository.save(new User(null, "Booker", "booker@example.com"));

        item = itemRepository.create(new Item(
                null, owner.getId(), "ownerItem", "This item for test", null, true
        ));
        // Создаём завершённое бронирование
        completedBooking = new Booking(
                null, booker.getId(), item.getId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                BookingStatus.COMPLETED
        );
    }

    private void activateCompletedBooking() {
        bookingRepository.addBooking(completedBooking);
    }

    @Test
    void testCreateComment_WithCompletedBooking_Success() throws Exception {
        // Дано: создаём завершённое бронирование прямо в тесте
        Booking completedBooking = new Booking(
                null, booker.getId(), item.getId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                BookingStatus.COMPLETED
        );
        bookingRepository.addBooking(completedBooking);

        CommentReqDto reqDto = new CommentReqDto("Отличный товар, всё понравилось!", true);

        // Когда: пользователь оставляет отзыв на вещь после завершённого бронирования
        mockMvc.perform(post("/comments")
                        .header("X-User-Id", booker.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDto)))
                // Тогда: отзыв успешно создан
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.bookerId", is(booker.getId().intValue())))
                .andExpect(jsonPath("$.itemId", is(item.getId().intValue())))
                .andExpect(jsonPath("$.comment", is("Отличный товар, всё понравилось!")));
    }

    @Test
    void testCreateComment_Duplicate_Error() throws Exception {
        // Дано: создаём завершённое бронирование для успешного создания первого отзыва
        Booking completedBooking = new Booking(
                null, booker.getId(), item.getId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                BookingStatus.COMPLETED
        );
        bookingRepository.addBooking(completedBooking);

        // И: уже существующий отзыв от этого пользователя на эту вещь
        CommentReqDto firstCommentDto = new CommentReqDto("Первый отзыв", true);
        mockMvc.perform(post("/comments")
                        .header("X-User-Id", booker.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(firstCommentDto)))
                .andExpect(status().isCreated());

        // И: пытаемся создать второй отзыв на ту же вещь от того же пользователя
        CommentReqDto secondCommentDto = new CommentReqDto("Второй отзыв", true);

        // Когда: отправляем запрос на создание дублирующего отзыва
        mockMvc.perform(post("/comments")
                        .header("X-User-Id", booker.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(secondCommentDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Пользователь уже оставил отзыв на эту вещь")));
    }

    @Test
    void testUpdateComment_OnlyAuthor_Success() throws Exception {
        // Дано: существующий отзыв
        activateCompletedBooking();
        CommentReqDto createDto = new CommentReqDto("Оригинальный текст отзыва", true);
        CommentSendDto createdComment = objectMapper.readValue(
                mockMvc.perform(post("/comments")
                                .header("X-User-Id", booker.getId())
                                .param("itemId", item.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createDto)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CommentSendDto.class
        );

        // Когда: автор обновляет свой отзыв
        CommentReqDto updateDto = new CommentReqDto("Обновлённый текст отзыва", true);
        mockMvc.perform(patch("/comments/" + createdComment.getId())
                        .header("X-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateDto)))
                // Тогда: отзыв успешно обновлён
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment", is("Обновлённый текст отзыва")));
    }


    @Test
    void testDeleteComment_OnlyAuthor_Success() throws Exception {
        // Дано: существующий отзыв

        // Создаём завершённое бронирование
        completedBooking = new Booking(
                null, booker.getId(), item.getId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                BookingStatus.COMPLETED
        );
        bookingRepository.addBooking(completedBooking);
        CommentReqDto createDto = new CommentReqDto("Отзыв для удаления", true);
        CommentSendDto createdComment = objectMapper.readValue(
                mockMvc.perform(post("/comments")
                                .header("X-User-Id", booker.getId())
                                .param("itemId", item.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createDto)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CommentSendDto.class
        );

        // Когда: автор удаляет свой отзыв
        mockMvc.perform(delete("/comments/" + createdComment.getId())
                        .header("X-User-Id", booker.getId()))
                // Тогда: отзыв удалён успешно
                .andExpect(status().isNoContent());

        // Проверяем, что отзыв действительно удалён
        mockMvc.perform(get("/comments/item/" + item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetCommentsByItem_Success() throws Exception {
        // Дано: создаём завершённое бронирование для booker (более раннее)
        Booking completedBooking1 = new Booking(
                null, booker.getId(), item.getId(),
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(3),
                BookingStatus.COMPLETED
        );
        bookingRepository.addBooking(completedBooking1);

        CommentReqDto comment1 = new CommentReqDto("Первый отзыв", true);

        // Создаём второго пользователя
        User anotherUser = userRepository.save(new User(null, "Another", "another@example.com"));

        // И его завершённое бронирование (в другое время, без пересечения)
        Booking completedBooking2 = new Booking(
                null, anotherUser.getId(), item.getId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                BookingStatus.COMPLETED
        );
        bookingRepository.addBooking(completedBooking2);

        CommentReqDto comment2 = new CommentReqDto("Второй отзыв", true);

        // Когда: создаём два отзыва от разных пользователей
        mockMvc.perform(post("/comments")
                        .header("X-User-Id", booker.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(comment1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/comments")
                        .header("X-User-Id", anotherUser.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(comment2)))
                .andExpect(status().isCreated());

        // Затем: запрашиваем все отзывы для вещи
        MvcResult result = mockMvc.perform(get("/comments/item/" + item.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // Тогда: получаем список из двух отзывов
        String responseBody = result.getResponse().getContentAsString();
        List<CommentSendDto> comments = objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructCollectionType(Collection.class, CommentSendDto.class)
        );

        assertThat(comments).hasSize(2);
    }


    @Test
    void testCreateComment_NoBooking_Error() throws Exception {
        // Дано: нет никаких бронирований для этой вещи и пользователя
        CommentReqDto reqDto = new CommentReqDto("Отзыв без бронирования", true);

        // Когда: пытаемся оставить отзыв без завершённого бронирования
        mockMvc.perform(post("/comments")
                        .header("X-User-Id", booker.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Пользователь не имеет завершённого бронирования")));
    }

    @Test
    void testCreateComment_WrongBookingStatus_Error() throws Exception {
        // Дано: бронирование с статусом WAITING (не COMPLETED)
        Booking waitingBooking = new Booking(
                null, booker.getId(), item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                BookingStatus.WAITING
        );
        bookingRepository.addBooking(waitingBooking);

        CommentReqDto reqDto = new CommentReqDto("Отзыв с неправильным статусом брони", true);

        // Когда: пытаемся оставить отзыв с неправильным статусом бронирования
        mockMvc.perform(post("/comments")
                        .header("X-User-Id", booker.getId())
                        .param("itemId", item.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDto)))
                // Тогда: ошибка 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Пользователь не имеет завершённого бронирования")));
    }

    @Test
    void testUpdateComment_UnauthorizedUser_Error() throws Exception {
        // Дано: существующий отзыв
        activateCompletedBooking();
        CommentReqDto createDto = new CommentReqDto("Оригинальный текст отзыва", true);
        CommentSendDto createdComment = objectMapper.readValue(
                mockMvc.perform(post("/comments")
                                .header("X-User-Id", booker.getId())
                                .param("itemId", item.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createDto)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CommentSendDto.class
        );

        // И: другой пользователь пытается обновить отзыв
        User unauthorizedUser = userRepository.save(new User(null, "Unauthorized", "unauth@example.com"));
        CommentReqDto updateDto = new CommentReqDto("Попытка изменения чужого отзыва", true);

        // Когда: отправляем запрос на обновление от чужого аккаунта
        mockMvc.perform(patch("/comments/" + createdComment.getId())
                        .header("X-User-Id", unauthorizedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateDto)))
                // Тогда: ошибка 403 Forbidden
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Только автор может редактировать свой отзыв")));
    }

    @Test
    void testDeleteComment_UnauthorizedUser_Error() throws Exception {
        // Дано: существующий отзыв
        activateCompletedBooking();
        CommentReqDto createDto = new CommentReqDto("Отзыв для удаления", true);
        CommentSendDto createdComment = objectMapper.readValue(
                mockMvc.perform(post("/comments")
                                .header("X-User-Id", booker.getId())
                                .param("itemId", item.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createDto)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CommentSendDto.class
        );

        // И: другой пользователь пытается удалить отзыв
        User unauthorizedUser = userRepository.save(new User(null, "Unauthorized", "unauth@example.com"));

        // Когда: отправляем запрос на удаление от чужого аккаунта
        mockMvc.perform(delete("/comments/" + createdComment.getId())
                        .header("X-User-Id", unauthorizedUser.getId()))
                // Тогда: ошибка 403 Forbidden
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Попытка пользователя ID: 3 удалить комментарий ID: 1 пользователя ID: 2")));
    }

    @Test
    void testGetCommentsByItem_NoComments_Success() throws Exception {
        // Дано: вещь без отзывов

        // Когда: запрашиваем отзывы для вещи без отзывов
        MvcResult result = mockMvc.perform(get("/comments/item/" + item.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // Тогда: получаем пустой список
        String responseBody = result.getResponse().getContentAsString();
        List<CommentSendDto> comments = objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CommentSendDto.class)
        );

        assertThat(comments).isEmpty();
    }
}

