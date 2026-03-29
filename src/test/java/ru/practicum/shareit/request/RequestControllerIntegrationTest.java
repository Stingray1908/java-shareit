/*package ru.practicum.shareit.request;

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
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.repository.InMemoryRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.InMemoryUserRepository;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private InMemoryRequestRepository requestRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        requestRepository.clear();
        userRepository.clear();
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

    // Создание DTO запроса с заданными параметрами
    private ItemRequestReqDTO createRequestDto(String description) {
        ItemRequestReqDTO request = new ItemRequestReqDTO();
        request.setDescription(description);
        return request;
    }

    // Основной тест: успешное создание запроса
    @Test
    public void testCreateRequest_Success() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemRequestReqDTO reqDTO = createRequestDto("Need a fan for summer");

        mockMvc.perform(post("/requests")
                        .header("X-Requestor-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.requestor", is(1)))
                .andExpect(jsonPath("$.description", is("Need a fan for summer")));
    }

    // Тест: создание запроса с пустым описанием
    @Test
    void createRequestWhenDescriptionIsNull_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemRequestReqDTO reqDTO = createRequestDto(null);
        performCreateRequestAndExpect(reqDTO, "1", HttpStatus.BAD_REQUEST);
    }

    // Тест: создание запроса для несуществующего пользователя
    @Test
    void createRequestWhenUserDoNotExist_NotFound() throws Exception {
        ItemRequestReqDTO reqDTO = createRequestDto("Need a laptop");
        performCreateRequestAndExpect(reqDTO, "999", HttpStatus.NOT_FOUND);
    }

    // Вспомогательный метод: выполнение POST-запроса и ожидание статуса
    private void performCreateRequestAndExpect(
            ItemRequestReqDTO reqDTO, String userId, HttpStatus expectedStatus) throws Exception {
        mockMvc.perform(post("/requests")
                        .header("X-Requestor-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(reqDTO)))
                .andExpect(status().is(expectedStatus.value()));
    }

    // Тест: успешное получение запроса по ID
    @Test
    void testGetRequestById_Success() throws Exception {
        // Arrange: создаём пользователя и запрос
        createTestUser("Christof", "christof@example.com");
        ItemRequestReqDTO createDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(createDto, "1", HttpStatus.CREATED);

        // Act & Assert: выполняем GET-запрос на получение запроса по ID
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.requestor", is(1)))
                .andExpect(jsonPath("$.description", is("Need a fan")));
    }

    // Тест: получение несуществующего запроса
    @Test
    void testGetRequestById_NotFound() throws Exception {
        mockMvc.perform(get("/requests/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // Тест: получение всех запросов пользователя
    @Test
    void testGetAllRequestsByUser_Success() throws Exception {
        // Arrange: создаём пользователя и два запроса
        createTestUser("Christof", "christof@example.com");

        ItemRequestReqDTO request1 = createRequestDto("Need a fan");
        ItemRequestReqDTO request2 = createRequestDto("Need a laptop");

        performCreateRequestAndExpect(request1, "1", HttpStatus.CREATED);
        performCreateRequestAndExpect(request2, "1", HttpStatus.CREATED);

        // Act & Assert: получаем список запросов пользователя
        mockMvc.perform(get("/requests")
                        .header("X-Requestor-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2))) // Проверяем, что вернули 2 запроса
                .andExpect(jsonPath("$[0].description", is("Need a fan")))
                .andExpect(jsonPath("$[1].description", is("Need a laptop")));
    }

    // Тест: получение запросов для несуществующего пользователя (пустой массив)
    @Test
    void testGetAllRequestsByNonExistentUser_EmptyArray() throws Exception {
        mockMvc.perform(get("/requests")
                        .header("X-Requestor-User-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // Тест: успешное удаление запроса
    @Test
    void testDeleteRequest_Success() throws Exception {
        // Arrange: создаём пользователя и запрос
        createTestUser("Christof", "christof@example.com");
        ItemRequestReqDTO createDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(createDto, "1", HttpStatus.CREATED);

        // Act 1: проверяем, что запрос существует
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Act 2: удаляем запрос
        mockMvc.perform(delete("/requests/1")
                        .header("X-Requestor-User-Id", "1"))
                .andExpect(status().isNoContent());

        // Act 3 & Assert: проверяем, что запрос больше не существует
        mockMvc.perform(get("/requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // Тест: попытка удаления запроса, который не принадлежит пользователю
    @Test
    void testDeleteRequest_NotOwnedByUser_Forbidden() throws Exception {
        // Arrange: создаём двух пользователей и запрос от первого
        createTestUser("Christof", "christof@example.com"); // ID 1
        createTestUser("Alice", "alice@example.com");     // ID 2

        ItemRequestReqDTO createDto = createRequestDto("Need a fan");
        performCreateRequestAndExpect(createDto, "1", HttpStatus.CREATED);

        // Act: пытаемся удалить запрос от имени другого пользователя (ID 2)
        mockMvc.perform(delete("/requests/1")
                        .header("X-Requestor-User-Id", "2"))
                .andExpect(status().isBadRequest());
    }

    // Тест: попытка удаления несуществующего запроса
    @Test
    void testDeleteRequest_NonExistent_NotFound() throws Exception {
        // Arrange: создаём пользователя
        createTestUser("Christof", "christof@example.com");

        // Act & Assert: пытаемся удалить запрос с несуществующим ID
        mockMvc.perform(delete("/requests/999")
                        .header("X-Requestor-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    // Тест: получение всех запросов в системе (административный эндпоинт)
    @Test
    void testGetAllRequests_AdminEndpoint_Success() throws Exception {
        // Arrange: создаём двух пользователей и несколько запросов
        createTestUser("Christof", "christof@example.com"); // ID 1
        createTestUser("Alice", "alice@example.com");     // ID 2

        ItemRequestReqDTO request1 = createRequestDto("Need a fan from Christof");
        ItemRequestReqDTO request2 = createRequestDto("Need a laptop from Christof");
        ItemRequestReqDTO request3 = createRequestDto("Need a book from Alice");

        performCreateRequestAndExpect(request1, "1", HttpStatus.CREATED);
        performCreateRequestAndExpect(request2, "1", HttpStatus.CREATED);
        performCreateRequestAndExpect(request3, "2", HttpStatus.CREATED);

        // Act & Assert: получаем все запросы в системе
        mockMvc.perform(get("/requests/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3))) // Всего 3 запроса
                .andExpect(jsonPath("$[0].description", is("Need a fan from Christof")))
                .andExpect(jsonPath("$[1].description", is("Need a laptop from Christof")))
                .andExpect(jsonPath("$[2].description", is("Need a book from Alice")))
                .andExpect(jsonPath("$[*].requestor", containsInAnyOrder(1, 1, 2)));
    }

    // Тест: получение всех запросов, когда в системе нет запросов
    @Test
    void testGetAllRequests_EmptySystem_EmptyArray() throws Exception {
        // Act & Assert: получаем все запросы — ожидаем пустой массив
        mockMvc.perform(get("/requests/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    // Тест: валидация описания запроса — слишком короткое описание
    @Test
    void createRequestWhenDescriptionTooShort_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        // Описание длиной 1 символ — должно вызвать ошибку валидации
        ItemRequestReqDTO reqDTO = createRequestDto("A");
        performCreateRequestAndExpect(reqDTO, "1", HttpStatus.BAD_REQUEST);
    }

    // Тест: валидация описания запроса — описание только из пробелов
    @Test
    void createRequestWhenDescriptionOnlySpaces_BadRequest() throws Exception {
        createTestUser("Christof", "christof@example.com");
        ItemRequestReqDTO reqDTO = createRequestDto("   ");
        performCreateRequestAndExpect(reqDTO, "1", HttpStatus.BAD_REQUEST);
    }
}
*/
