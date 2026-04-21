package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.HttpHeader;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void create_shouldReturnCreatedItem() throws Exception {
        Long ownerId = 1L;
        ItemReqDTO itemReqDTO = new ItemReqDTO();
        itemReqDTO.setName("Test Item");
        itemReqDTO.setDescription("Test description");
        itemReqDTO.setAvailable(true);

        ItemSendDTO expectedItem = new ItemSendDTO();
        expectedItem.setId(1L);
        expectedItem.setName("Test Item");
        expectedItem.setDescription("Test description");
        expectedItem.setAvailable(true);

        when(itemService.create(anyLong(), any(ItemReqDTO.class))).thenReturn(expectedItem);

        mockMvc.perform(post("/items")
                        .header(HttpHeader.X_SHARER_USER_ID, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReqDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Item"))
                .andExpect(jsonPath("$.description").value("Test description"));

        verify(itemService, times(1)).create(eq(ownerId), any(ItemReqDTO.class));
    }

    @Test
    void addComment_shouldReturnCreatedComment() throws Exception {
        Long itemId = 1L;
        Long userId = 2L;

        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setText("Great item!");

        CommentSendDto expectedComment = new CommentSendDto();
        expectedComment.setId(1L);
        expectedComment.setText("Great item!");
        expectedComment.setCreated(now);

        when(itemService.addComment(anyLong(), anyLong(), any(CommentReqDto.class)))
                .thenReturn(expectedComment);

        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReqDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Great item!"));

        verify(itemService, times(1)).addComment(eq(userId), eq(itemId), any(CommentReqDto.class));
    }

    @Test
    void update_shouldReturnUpdatedItem() throws Exception {
        Long itemId = 1L;
        Long userId = 1L;

        ItemReqDTO itemReqDTO = new ItemReqDTO();
        itemReqDTO.setName("Updated Name");
        itemReqDTO.setDescription("Updated description");

        ItemSendDTO updatedItem = new ItemSendDTO();
        updatedItem.setId(itemId);
        updatedItem.setName("Updated Name");
        updatedItem.setDescription("Updated description");
        updatedItem.setAvailable(true);

        when(itemService.update(anyLong(), anyLong(), any(ItemReqDTO.class)))
                .thenReturn(updatedItem);

        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"));

        verify(itemService, times(1)).update(eq(itemId), eq(userId), any(ItemReqDTO.class));
    }

    @Test
    void getById_shouldReturnItem() throws Exception {
        Long itemId = 1L;

        ItemSendDTO item = new ItemSendDTO();
        item.setId(itemId);
        item.setName("Test Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setLastBooking(now.minusDays(1));
        item.setNextBooking(now.plusDays(1));

        when(itemService.getById(anyLong())).thenReturn(item);

        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Test Item"))
                .andExpect(jsonPath("$.lastBooking").exists())
                .andExpect(jsonPath("$.nextBooking").exists());

        verify(itemService, times(1)).getById(eq(itemId));
    }

    @Test
    void getOwnerItems_shouldReturnItemsList() throws Exception {
        Long ownerId = 1L;

        ItemSendDTO item1 = new ItemSendDTO();
        item1.setId(1L);
        item1.setName("Item 1");
        item1.setDescription("Desc 1");
        item1.setAvailable(true);

        ItemSendDTO item2 = new ItemSendDTO();
        item2.setId(2L);
        item2.setName("Item 2");
        item2.setDescription("Desc 2");
        item2.setAvailable(false);

        List<ItemSendDTO> items = List.of(item1, item2);

        when(itemService.getOwnerItems(anyLong())).thenReturn(items);

        mockMvc.perform(get("/items")
                        .header(HttpHeader.X_SHARER_USER_ID, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(itemService, times(1)).getOwnerItems(eq(ownerId));
    }

    @Test
    void getRequestItemsById_shouldReturnItemsForRequest() throws Exception {
        Long requestId = 1L;
        Long ownerId = 1L;

        ItemSendDTO item = new ItemSendDTO();
        item.setId(1L);
        item.setName("Requested Item");
        item.setDescription("Description");

        List<ItemSendDTO> items = Collections.singletonList(item);

        when(itemService.findItemsByRequestIdForRequester(anyLong(), anyLong()))
                .thenReturn(items);

        mockMvc.perform(get("/items/request/{id}", requestId)
                        .header(HttpHeader.X_SHARER_USER_ID, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Requested Item"));

        verify(itemService, times(1)).findItemsByRequestIdForRequester(eq(requestId), eq(ownerId));
    }

    @Test
    void deleteByItemAndOwnerIds_shouldDeleteItem() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;

        doNothing().when(itemService).deleteByIdForOwner(anyLong(), anyLong());

        mockMvc.perform(delete("/items/{itemId}", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId))
                .andExpect(status().isNoContent());

        verify(itemService, times(1)).deleteByIdForOwner(eq(userId), eq(itemId));
    }

    @Test
    void search_shouldReturnSearchResults() throws Exception {
        String searchText = "test";

        ItemSendDTO item1 = new ItemSendDTO();
        item1.setId(1L);
        item1.setName("Test Item 1");
        item1.setDescription("Description with test word");
        item1.setAvailable(true);

        ItemSendDTO item2 = new ItemSendDTO();
        item2.setId(2L);
        item2.setName("Another Test Item");
        item2.setDescription("Another description");
        item2.setAvailable(false);

        List<ItemSendDTO> searchResults = List.of(item1, item2);

        when(itemService.search(anyString())).thenReturn(searchResults);

        mockMvc.perform(get("/items/search")
                        .param("text", searchText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                // Проверяем точное соответствие имени
                .andExpect(jsonPath("$[0].name").value("Test Item 1"))
                .andExpect(jsonPath("$[1].name").value("Another Test Item"))
                // Дополнительно проверяем, что имя содержит подстроку "Test"
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertTrue(content.contains("\"name\":\"Test Item 1\""));
                    assertTrue(content.contains("\"name\":\"Another Test Item\""));
                });

        verify(itemService, times(1)).search(eq(searchText));
    }


    @Test
    void search_withEmptyText_shouldReturnEmptyList() throws Exception {
        String emptyText = "";

        when(itemService.search(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/items/search")
                        .param("text", emptyText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(itemService, times(1)).search(eq(emptyText));
    }

    @Test
    void addComment_whenUserHasNotBookedItem_shouldReturnBadRequest() throws Exception {
        Long itemId = 1L;
        Long userId = 2L;

        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setText("Great item!");

        // Симулируем исключение, которое выбрасывается, когда пользователь не брал вещь в аренду
        when(itemService.addComment(anyLong(), anyLong(), any(CommentReqDto.class)))
                .thenThrow(new IllegalArgumentException(
                        "Пользователь не брал эту вещь в аренду, поэтому не может оставить отзыв"));

        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReqDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, times(1)).addComment(eq(userId), eq(itemId), any(CommentReqDto.class));
    }

    @Test
    void update_whenNoFieldsToUpdate_shouldReturnBadRequest() throws Exception {
        Long itemId = 1L;
        Long userId = 1L;

        ItemReqDTO itemReqDTO = new ItemReqDTO(); // пустой DTO — нет полей для обновления

        // Настраиваем мок: при вызове update выбрасываем исключение
        when(itemService.update(eq(itemId), eq(userId), any(ItemReqDTO.class)))
                .thenThrow(new IllegalArgumentException("При обновлении хотя бы одно поле должно быть заполнено"));

        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReqDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.message").value("При обновлении хотя бы одно поле должно быть заполнено"))
                .andExpect(jsonPath("$.status").value(400));

        // Проверяем, что сервис был вызван ровно один раз с ожидаемыми параметрами
        verify(itemService, times(1)).update(eq(itemId), eq(userId), any(ItemReqDTO.class));
    }

    @Test
    void addComment_shouldCreateCommentSuccessfully() throws Exception {
        Long itemId = 1L;
        Long userId = 2L;

        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setText("Отличный товар!");

        CommentSendDto expectedComment = new CommentSendDto();
        expectedComment.setId(1L);
        expectedComment.setText("Отличный товар!");
        expectedComment.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        when(itemService.addComment(eq(userId), eq(itemId), any(CommentReqDto.class)))
                .thenReturn(expectedComment);

        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReqDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Отличный товар!"))
                .andExpect(jsonPath("$.created").exists());

        verify(itemService, times(1)).addComment(eq(userId), eq(itemId), any(CommentReqDto.class));
    }

    @Test
    void addComment_withEmptyText_shouldReturnBadRequest() throws Exception {
        Long itemId = 1L;
        Long userId = 2L;

        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setText(""); // пустой текст

        // Настраиваем мок: при вызове addComment выбрасываем исключение
        when(itemService.addComment(eq(userId), eq(itemId), any(CommentReqDto.class)))
                .thenThrow(new IllegalArgumentException("Текст комментария не может быть пустым"));

        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header(HttpHeader.X_SHARER_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReqDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.message").value("Текст комментария не может быть пустым"))
                .andExpect(jsonPath("$.status").value(400));

        // Проверяем, что сервис был вызван ровно один раз с ожидаемыми параметрами
        verify(itemService, times(1)).addComment(eq(userId), eq(itemId), any(CommentReqDto.class));
    }

}

