/*package ru.practicum.shareit.item;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.repository.InMemoryItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.repository.InMemoryRequestRepository;
import ru.practicum.shareit.request.service.RequestService;
import ru.practicum.shareit.request.service.RequestServiceImpl;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.InMemoryUserRepository;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerIntegrationTest {

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private InMemoryItemRepository itemRepository;

    @Autowired
    private InMemoryRequestRepository requestRepository;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        itemRepository.clear();
        userRepository.clear();
        requestRepository.clear();
    }

    // Вспомогательный метод для сериализации в JSON
    private String asJsonString(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    // Создание тестового пользователя
    private User createTestUser(String name, String email) {
        User user = new User(null, name, email);
        userRepository.save(user);
        return user;
    }

    // Создание DTO вещи с заданными параметрами
    private ItemReqDTO createItemDto(String name, String description, Boolean available) {
        ItemReqDTO item = new ItemReqDTO();
        item.setName(name);
        item.setDescription(description);
        item.setAvailable(available);
        return item;
    }

    // Основной тест: успешное создание вещи
    @Test
    public void testCreateItem_Success() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO reqDTO = createItemDto("Fan", "For drying hair", true);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.ownerId", is(1)))
                .andExpect(jsonPath("$.name", is("Fan")))
                .andExpect(jsonPath("$.description", is("For drying hair")))
                .andExpect(jsonPath("$.available", is(true)));
    }

    // Тесты на ошибки при создании вещи
    @Test
    public void createItemWhenNameIsNull_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO reqDTO = createItemDto(null, "For drying hair", true);
        performCreateRequestAndExpect(reqDTO, "1", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void createItemWhenDescriptionIsNull_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO reqDTO = createItemDto("Fan", null, true);
        performCreateRequestAndExpect(reqDTO, "1", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void createItemWhenAvailableIsNull_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO reqDTO = createItemDto("Fan", "For drying hair", null);
        performCreateRequestAndExpect(reqDTO, "1", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void createItemWhenUserDoNotExist_BadRequest() throws Exception {
        ItemReqDTO reqDTO = createItemDto("Fan", "For drying hair", true);
        performCreateRequestAndExpect(reqDTO, "999", HttpStatus.NOT_FOUND);
    }

    // Вспомогательный метод: выполнение POST-запроса и ожидание статуса
    private void performCreateRequestAndExpect(ItemReqDTO reqDTO, String userId, HttpStatus expectedStatus) throws Exception {
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDTO)))
                .andExpect(status().is(expectedStatus.value()));
    }

    private void performCreateRequestAndExpect(ItemRequestReqDTO reqDTO, int userId, HttpStatus expectedStatus) throws Exception {
        mockMvc.perform(post("/requests")
                        .header("X-Requestor-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDTO)))
                .andExpect(status().is(expectedStatus.value()));
    }

    // Тест: успешное обновление вещи (PATCH)
    @Test
    public void testPatchItem_Success() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO createDto = createItemDto("Fan", "For drying hair", true);
        performCreateRequestAndExpect(createDto, "1", HttpStatus.CREATED);

        ItemReqDTO patchDto = new ItemReqDTO();
        patchDto.setName("Vacuum cleaner");
        patchDto.setDescription("To absorb dust");
        patchDto.setAvailable(false);

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Vacuum cleaner")))
                .andExpect(jsonPath("$.description", is("To absorb dust")))
                .andExpect(jsonPath("$.available", is(false)));
    }

    // Тест: обновление несуществующей вещи
    @Test
    public void testPatchItem_NotFound() throws Exception {
        createTestUser("Christof", "christof@example.com");

        ItemReqDTO patchDto = createItemDto("Vacuum cleaner", "To absorb dust", false);

        mockMvc.perform(patch("/items/999")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchDto)))
                .andExpect(status().isNotFound());
    }

    // Тест: все поля для обновления — null
    @Test
    public void testPatchItem_AllFieldsNull_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO createDto = createItemDto("Fan", "For drying hair", true);
        performCreateRequestAndExpect(createDto, "1", HttpStatus.CREATED);

        ItemReqDTO patchDto = new ItemReqDTO();
        patchDto.setName(null);
        patchDto.setDescription(null);
        patchDto.setAvailable(null);

        // Запрос на обновление
        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchDto)))
                .andExpect(status().isBadRequest()); // Ожидаем статус 400 Bad Request
    }


    @Test
    public void testGetItemById_Success() throws Exception {
        // Arrange: создаём тестового пользователя и вещь
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO createDto = createItemDto("Fan", "For drying hair", true);
        performCreateRequestAndExpect(createDto, "1", HttpStatus.CREATED);

        // Act & Assert: выполняем GET‑запрос на получение вещи по ID
        mockMvc.perform(get("/items/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Fan")))
                .andExpect(jsonPath("$.description", is("For drying hair")))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.ownerId", is(1)));
    }

    @Test
    public void testGetOwnerItems_TwoItemsAvailable() throws Exception {
        // Arrange: создаём пользователя и две вещи
        createTestUser("Christof", "christof@example.com");

        ItemReqDTO item1 = createItemDto("Fan", "For drying hair", true);
        ItemReqDTO item2 = createItemDto("Laptop", "For working", false);

        performCreateRequestAndExpect(item1, "1", HttpStatus.CREATED);
        performCreateRequestAndExpect(item2, "1", HttpStatus.CREATED);

        // Act & Assert: получаем список вещей пользователя
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2))) // Проверяем, что вернули 2 вещи
                .andExpect(jsonPath("$[0].name", is("Fan")))
                .andExpect(jsonPath("$[0].available", is(true)))
                .andExpect(jsonPath("$[1].name", is("Laptop")))
                .andExpect(jsonPath("$[1].available", is(false)));
    }

    @Test
    public void testDeleteItem_ItemsCountDecreases() throws Exception {
        // Arrange: создаём пользователя и две вещи
        createTestUser("Christof", "christof@example.com");

        ItemReqDTO item1 = createItemDto("Fan", "For drying hair", true);
        ItemReqDTO item2 = createItemDto("Laptop", "For working", false);

        performCreateRequestAndExpect(item1, "1", HttpStatus.CREATED);
        performCreateRequestAndExpect(item2, "1", HttpStatus.CREATED);

        // Act 1: проверяем, что изначально есть 2 вещи
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));

        // Act 2: удаляем одну вещь (например, с ID 1)
        mockMvc.perform(delete("/items/1")
                        .header("X-Sharer-User-Id", 1))
                .andExpect(status().isNoContent());

        // Act 3 & Assert: проверяем, что осталась только 1 вещь
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(2)))
                .andExpect(jsonPath("$[0].name", is("Laptop")));
    }

    @Test
    public void testCreateItemWithNonExistentRequestId_ThrowsException() throws Exception {
        // Arrange: создаём пользователя, но не создаём запрос
        createTestUser("Christof", "christof@example.com");
        ItemReqDTO reqDTO = createItemDto("Fan", "For drying hair", true);
        reqDTO.setRequestId(999L); // Несуществующий requestId

        // Act & Assert: ожидаем ошибку 400 Bad Request
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDTO)))
                .andExpect(status().isNotFound());
    }

    private ItemRequestReqDTO createRequestDto(String description) {
        ItemRequestReqDTO request = new ItemRequestReqDTO();
        request.setDescription(description);
        return request;
    }

    @Test                  // ok
    public void testCreateItemWithRequestId_PendingStatusChangesToResponded() throws Exception {
        // Arrange: создаём пользователя и запрос со статусом PENDING
        createTestUser("Christof", "christof@example.com");

        ItemRequestReqDTO requestDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        // Создаём вещь с requestId
        ItemReqDTO itemDto = createItemDto("Fan", "For drying hair", true);
        itemDto.setRequestId(1L);

        // Act: создаём вещь
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(itemDto)))
                .andExpect(status().isCreated());

        // Assert: проверяем, что статус запроса изменился на RESPONDED
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESPONDED")));


    }

    @Test
    public void testCreateItemWithRequestId_NonPendingStatusDoesNotChange() throws Exception {
        // Arrange: создаём пользователя и запрос
        createTestUser("Christof", "christof@example.com");

        ItemRequestReqDTO requestDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        // Вручную меняем статус запроса на CANCELLED
        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.CANCELLED);
        requestService.patchStatus(patchDto, 1L, 1L);

        // Проверяем, что статус изменился на CANCELLED
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Создаём вещь с requestId
        ItemReqDTO itemDto = createItemDto("Fan", "For drying hair", true);
        itemDto.setRequestId(1L);

        // Act: создаём вещь — ожидаем ошибку 400, так как запрос CANCELLED
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(itemDto)))
                .andExpect(status().isCreated());
        // Assert: проверяем, что статус остался CANCELLED
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    public void testCreateItemWithoutRequestId_StatusDoesNotChange() throws Exception {
        // Arrange: создаём пользователя и запрос со статусом PENDING
        createTestUser("Christof", "christof@example.com");

        ItemRequestReqDTO requestDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        // Создаём вещь без requestId
        ItemReqDTO itemDto = createItemDto("Fan", "For drying hair", true);

        // Act: создаём вещь
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(itemDto)))
                .andExpect(status().isCreated());

        // Assert: проверяем, что статус запроса остался PENDING
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    public void testGetRequestItemsById_SuccessWithAddAndDelete() throws Exception {
        // Arrange: создаём пользователя и запрос
        User user = createTestUser("Christof", "christof@example.com");
        ItemRequestReqDTO requestDto = createRequestDto("Need a fan and laptop");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        // Создаём две вещи с requestId=1
        ItemReqDTO item1 = createItemDto("Fan", "For drying hair", true);
        item1.setRequestId(1L);
        ItemReqDTO item2 = createItemDto("Laptop", "For working", false);
        item2.setRequestId(1L);

        performCreateRequestAndExpect(item1, "1", HttpStatus.CREATED);
        performCreateRequestAndExpect(item2, "1", HttpStatus.CREATED);

        // Act 1: получаем вещи для запроса ID=1
        mockMvc.perform(get("/items/request/1")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].name", is("Fan")))
                .andExpect(jsonPath("$[1].name", is("Laptop")));

        // Act 2: удаляем одну вещь (ID=1)
        mockMvc.perform(delete("/items/1")
                        .header("X-Sharer-User-Id", 1))
                .andExpect(status().isNoContent());

        // Act 3 & Assert: проверяем, что осталась только одна вещь
        mockMvc.perform(get("/items/request/1")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(2)))
                .andExpect(jsonPath("$[0].name", is("Laptop")));
    }

    @Test
    public void testGetRequestItemsById_RequestDeletedAfterItemCreation() throws Exception {
        // Arrange: создаём пользователя, запрос и вещь
        createTestUser("Christof", "christof@example.com");

        ItemRequestReqDTO requestDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        ItemReqDTO itemDto = createItemDto("Fan", "For drying hair", true);
        itemDto.setRequestId(1L);
        performCreateRequestAndExpect(itemDto, "1", HttpStatus.CREATED);

        // Удаляем запрос (предполагаем, что есть endpoint для удаления запросов)
        mockMvc.perform(delete("/requests/1")
                        .header("X-Requestor-User-Id", 1))
                .andExpect(status().isNoContent());

        // Act & Assert: пытаемся получить вещи для удалённого запроса
        mockMvc.perform(get("/items/request/1")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetRequestItemsById_NoItemsForRequest_ReturnsEmptyList() throws Exception {
        // Arrange: создаём пользователя и запрос без вещей
        createTestUser("Christof", "christof@example.com");

        ItemRequestReqDTO requestDto = createRequestDto("Need something");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        // Act & Assert: получаем вещи для запроса без привязанных вещей
        mockMvc.perform(get("/items/request/1")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)))
                .andExpect(jsonPath("$", is(empty())));
    }

    @Test
    public void testGetRequestItemsById_NonOwnerAccess_ThrowsException() throws Exception {
        // Arrange: создаём двух пользователей
        User owner = createTestUser("Owner", "owner@example.com");
        User nonOwner = createTestUser("NonOwner", "nonowner@example.com");

        // Создаём запрос от первого пользователя
        ItemRequestReqDTO requestDto = createRequestDto("Need a thing");
        performCreateRequestAndExpect(requestDto, 1, HttpStatus.CREATED);

        // Создаём вещь с requestId=1 от первого пользователя
        ItemReqDTO itemDto = createItemDto("Thing", "Description", true);
        itemDto.setRequestId(1L);
        performCreateRequestAndExpect(itemDto, "1", HttpStatus.CREATED);

        // Act & Assert: второй пользователь пытается получить вещи для запроса первого
        mockMvc.perform(get("/items/request/1")
                        .header("X-Sharer-User-Id", "2")  // ID второго пользователя
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());  // или другой подходящий статус
    }


}
*/
