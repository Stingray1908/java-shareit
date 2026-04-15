package ru.practicum.shareit.request.service;

import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.util.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.repository.ItemJpaRepository;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RequestJpaServiceTest {

    @Autowired
    private RequestJpaRepository requestRepository;

    @Autowired
    private UserJpaRepository userJPARepository;

    @Autowired
    private RequestService requestService;

    private Long requesterId;
    private static final String TEST_DESCRIPTION = "Test request description";

    @BeforeEach
    void setUp() {
        cleanDatabase();
        requesterId = createUser("Requester User", "requester@example.com");
    }

    private void cleanDatabase() {
        requestRepository.deleteAll();
        userJPARepository.deleteAll();
    }

    private Long createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userJPARepository.save(user).getId();
    }

    private ItemRequestReqDTO createRequestDto(String description) {
        ItemRequestReqDTO dto = new ItemRequestReqDTO();
        dto.setDescription(description);
        return dto;
    }

    private ItemRequestSendDTO createTestRequest(String description, Long requesterId) {
        ItemRequestReqDTO requestDto = createRequestDto(description);
        return requestService.create(requestDto, requesterId);
    }

    private void assertRequestFields(ItemRequestSendDTO result, String expectedDescription, Long expectedRequesterId, RequestStatus expectedStatus) {
        assertThat(result.getId()).isNotNull();
        assertThat(result.getDescription()).isEqualTo(expectedDescription);
        assertThat(result.getRequester().getId()).isEqualTo(expectedRequesterId);
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        assertThat(result.getCreated()).isNotNull();
    }

    private void assertSavedRequestInDb(Long requestId, String expectedDescription, Long expectedRequesterId, RequestStatus expectedStatus) {
        Optional<ru.practicum.shareit.request.ItemRequest> savedRequest = requestRepository.findById(requestId);
        assertThat(savedRequest).isPresent();

        ru.practicum.shareit.request.ItemRequest actualRequest = savedRequest.get();
        assertThat(actualRequest.getDescription()).isEqualTo(expectedDescription);
        assertThat(actualRequest.getRequester().getId()).isEqualTo(expectedRequesterId);
        assertThat(actualRequest.getStatus()).isEqualTo(expectedStatus);
        assertThat(actualRequest.getCreated()).isNotNull();
        assertThat(actualRequest.getCreated())
                .isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void create_ShouldSaveRequestWithAllValidFields_WhenDescriptionIsProvided() {
        // Given
        ItemRequestReqDTO requestDto = createRequestDto(TEST_DESCRIPTION);

        // When
        ItemRequestSendDTO result = requestService.create(requestDto, requesterId);

        // Then
        assertRequestFields(result, TEST_DESCRIPTION, requesterId, RequestStatus.PENDING);
        assertSavedRequestInDb(result.getId(), TEST_DESCRIPTION, requesterId, RequestStatus.PENDING);
    }

    @Test
    void create_ShouldThrowException_WhenUserDoesNotExist() {
        // Given
        Long nonExistentUserId = 999L;
        ItemRequestReqDTO requestDto = createRequestDto("Another test description");

        // When & Then
        assertThatThrownBy(() -> requestService.create(requestDto, nonExistentUserId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void patchStatus_ShouldThrowNoSuchElementException_WhenRequestDoesNotExist() {
        // Given
        Long nonExistentRequestId = 999L;
        Long validRequestorId = requesterId;
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setStatus(RequestStatus.COMPLETED);

        // When & Then
        assertThatThrownBy(() -> requestService.patchStatus(requestDto, nonExistentRequestId, validRequestorId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не существует или имеет неактивный статус");
    }

    @Test
    void patchStatus_ShouldThrowSecurityException_WhenUserHasNoAccess() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest("Original request", requesterId);
        Long unauthorizedUserId = createUser("Unauthorized User", "unauthorized@example.com");

        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.COMPLETED);

        // When & Then
        assertThatThrownBy(() -> requestService.patchStatus(patchDto, createdRequest.getId(), unauthorizedUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Попытка несанкционированного доступа");
    }

    @Test
    void patchStatus_ShouldThrowIllegalArgumentException_WhenInvalidStatusProvided() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest("Test request for status validation", requesterId);

        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.PENDING);

        // When & Then
        assertThatThrownBy(() -> requestService.patchStatus(patchDto, createdRequest.getId(), requesterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь пытается установить системный статус");

        // Дополнительно проверяем, что статус не изменился в БД
        assertSavedRequestInDb(createdRequest.getId(), "Test request for status validation", requesterId, RequestStatus.PENDING);
    }

    @Test
    void patchStatus_ShouldUpdateStatusSuccessfully_WhenValidStatusAndAccess() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest("Test request for successful status update", requesterId);

        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.COMPLETED);

        // When
        ItemRequestSendDTO result = requestService.patchStatus(patchDto, createdRequest.getId(), requesterId);

        // Then
        assertThat(result.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        assertSavedRequestInDb(createdRequest.getId(), "Test request for successful status update", requesterId, RequestStatus.COMPLETED);
    }

    @Test
    void getById_ShouldReturnRequest_WhenRequestExists() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest(TEST_DESCRIPTION, requesterId);

        // When
        ItemRequestSendDTO result = requestService.getById(createdRequest.getId());

        // Then
        assertRequestFields(result, TEST_DESCRIPTION, requesterId, RequestStatus.PENDING);
    }

    @Test
    void getById_ShouldThrowNoSuchElementException_WhenRequestDoesNotExist() {
        // Given
        Long nonExistentRequestId = 999L;

        // When & Then
        assertThatThrownBy(() -> requestService.getById(nonExistentRequestId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не существует");
    }

    @Test
    void deleteById_ShouldDeleteRequest_WhenRequestExistsAndUserHasAccess() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest(TEST_DESCRIPTION, requesterId);

        // When
        requestService.deleteById(createdRequest.getId(), requesterId);

        // Then
        assertThat(requestRepository.findById(createdRequest.getId())).isEmpty();
    }

    @Test
    void deleteById_ShouldThrowSecurityException_WhenUserHasNoAccess() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest("Original request", requesterId);
        Long unauthorizedUserId = createUser("Unauthorized User", "unauthorized@example.com");

        // When & Then
        assertThatThrownBy(() -> requestService.deleteById(createdRequest.getId(), unauthorizedUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Попытка несанкционированного доступа");

        // Дополнительно проверяем, что запрос остался в БД
        assertThat(requestRepository.findById(createdRequest.getId())).isPresent();
    }

    @Test
    void getAllByRequestorId_ShouldReturnAllRequests_WhenMultipleRequestsExist() {
        // Given
        ItemRequestSendDTO firstRequest = createTestRequest("First request description", requesterId);
        ItemRequestSendDTO secondRequest = createTestRequest("Second request description", requesterId);

        // When
        List<ItemRequestSendDTO> requests = requestService.getAllByRequestorId(requesterId);

        // Then
        assertThat(requests).hasSize(2);
        assertThat(requests)
                .extracting("description")
                .containsExactlyInAnyOrder("First request description", "Second request description");
    }

    @Test
    void getAllByRequestorId_ShouldReturnOneRequest_AfterOneIsDeleted() {
        // Given
        ItemRequestSendDTO firstRequest = createTestRequest("First request description", requesterId);
        ItemRequestSendDTO secondRequest = createTestRequest("Second request description", requesterId);

        // Удаляем один запрос
        requestService.deleteById(firstRequest.getId(), requesterId);

        // When
        List<ItemRequestSendDTO> remainingRequests = requestService.getAllByRequestorId(requesterId);

        // Then
        assertThat(remainingRequests).hasSize(1);
        assertThat(remainingRequests.getFirst().getId()).isEqualTo(secondRequest.getId());
        assertThat(remainingRequests.getFirst().getDescription()).isEqualTo("Second request description");
    }

    @Test
    void patchStatusInternal_ShouldUpdateToSystemStatusSuccessfully_WhenValidSystemStatus() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest("Test request for internal status update", requesterId);
        ru.practicum.shareit.request.ItemRequest requestEntity = requestRepository.findById(createdRequest.getId())
                .orElseThrow();
        requestEntity.setStatus(RequestStatus.RESPONDED);

        // When
        ru.practicum.shareit.request.ItemRequest result = requestService.patchStatusInternal(requestEntity);

        // Then
        assertThat(result.getStatus()).isEqualTo(RequestStatus.RESPONDED);
        assertSavedRequestInDb(createdRequest.getId(), "Test request for internal status update", requesterId, RequestStatus.RESPONDED);
    }

    @Test
    void patchStatusInternal_ShouldThrowInternalException_WhenTryingToSetCompletedStatus() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest("Test request for invalid internal status", requesterId);
        ru.practicum.shareit.request.ItemRequest requestEntity = requestRepository.findById(createdRequest.getId())
                .orElseThrow();
        requestEntity.setStatus(RequestStatus.COMPLETED);

        // When & Then
        assertThatThrownBy(() -> requestService.patchStatusInternal(requestEntity))
                .isInstanceOf(InternalException.class)
                .hasMessageContaining("Система пытается установить статус запроса");
    }

    @Test
    void create_ShouldThrowIllegalArgumentException_WhenDescriptionIsTooLong() {
        // Given
        String longDescription = "A".repeat(2000); // предположим, лимит 1000 символов
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription(longDescription);

        // When & Then
        assertThatThrownBy(() -> requestService.create(requestDto, requesterId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void patchStatus_ShouldThrowException_WhenRequestIsInactive() {
        // Given: создаём запрос и устанавливаем статус CANCELLED (считается неактивным)
        ItemRequestSendDTO createdRequest = createTestRequest("Test request for inactive status", requesterId);

        ItemRequestReqDTO initialPatchDto = new ItemRequestReqDTO();
        initialPatchDto.setStatus(RequestStatus.CANCELLED);
        requestService.patchStatus(initialPatchDto, createdRequest.getId(), requesterId);

        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.PENDING); // пытаемся установить новый статус для неактивного запроса

        // When & Then: ожидаем исключение при попытке изменить статус неактивного запроса
        assertThatThrownBy(() -> requestService.patchStatus(patchDto, createdRequest.getId(), requesterId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Запрос с id: " + createdRequest.getId() + " не существует или имеет неактивный статус");
    }

    @Test
    void patchStatus_ShouldThrowException_WhenUserTriesToSetSystemStatus() {
        // Given: создаём запрос в начальном состоянии
        ItemRequestSendDTO createdRequest = createTestRequest("Test request for system status restriction", requesterId);


        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.RESPONDED); // системный статус


        // When & Then: ожидаем исключение при попытке установить системный статус
        assertThatThrownBy(() -> requestService.patchStatus(patchDto, createdRequest.getId(), requesterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь пытается установить системный статус");
    }


    @Test
    void getAllByRequestorId_ShouldReturnEmptyList_WhenUserHasNoRequests() {
        // Given
        Long userWithoutRequests = createUser("User Without Requests", "noway@example.com");

        // When
        List<ItemRequestSendDTO> requests = requestService.getAllByRequestorId(userWithoutRequests);

        // Then
        assertThat(requests).isEmpty();
    }

    @Test
    void getById_ShouldThrowNoSuchElementException_WhenNegativeIdProvided() {
        // Given
        Long negativeId = -1L;

        // When & Then
        assertThatThrownBy(() -> requestService.getById(negativeId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не существует");
    }

    @Test
    void deleteById_ShouldThrowSecurityException_WhenZeroUserIdProvided() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest(TEST_DESCRIPTION, requesterId);
        Long zeroUserId = 0L;

        // When & Then
        assertThatThrownBy(() -> requestService.deleteById(createdRequest.getId(), zeroUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Попытка несанкционированного доступа");
    }

    @Test
    void create_ShouldSetStatusToPendingByDefault_WhenStatusNotProvidedInDto() {
        // Given
        ItemRequestReqDTO requestDto = createRequestDto(TEST_DESCRIPTION);
        requestDto.setStatus(null); // явно не передаём статус

        // When
        ItemRequestSendDTO result = requestService.create(requestDto, requesterId);

        // Then
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertSavedRequestInDb(result.getId(), TEST_DESCRIPTION, requesterId, RequestStatus.PENDING);
    }

    @Test
    void getAllWithItems_ShouldReturnAllRequestsWithAssociatedItems() {
        // Given: два пользователя, два запроса, один с вещами
        Long user1Id = createUser("User1", "user1@example.com");
        Long user2Id = createUser("User2", "user2@example.com");

        ItemRequestSendDTO request1 = createTestRequest("Request with items", user1Id);
        ItemRequestSendDTO request2 = createTestRequest("Request without items", user2Id);

        // Добавляем вещи к первому запросу
        addItemsToRequest(request1.getId(), 2); // 2 вещи

        // When
        List<ItemRequestWithItemsDto> result = requestService.getAllWithItems();

        // Then
        assertThat(result).hasSize(2);

        // Первый запрос — с двумя вещами
        var requestWithItems = result.stream()
                .filter(r -> r.getId().equals(request1.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(requestWithItems.getItems()).hasSize(2);

        // Второй запрос — без вещей
        var requestWithoutItems = result.stream()
                .filter(r -> r.getId().equals(request2.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(requestWithoutItems.getItems()).isEmpty();
    }

    @Test
    void create_ShouldAcceptMinimumLengthDescription() {
        // Given
        String minDescription = "A"; // минимальная длина (предположим, 1 символ)
        ItemRequestReqDTO requestDto = createRequestDto(minDescription);

        // When
        ItemRequestSendDTO result = requestService.create(requestDto, requesterId);

        // Then
        assertThat(result.getDescription()).isEqualTo(minDescription);
    }

    @Test
    void create_ShouldHandleConcurrentRequests() throws Exception {
        // Given
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Future<ItemRequestSendDTO>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<ItemRequestSendDTO> future = executor.submit(() -> {
                latch.countDown();
                latch.await(); // все потоки стартуют одновременно
                ItemRequestReqDTO dto = createRequestDto("Concurrent request " + System.currentTimeMillis());
                return requestService.create(dto, requesterId);
            });
            futures.add(future);
        }

        // When: выполняем все запросы параллельно
        List<ItemRequestSendDTO> results = new ArrayList<>();
        for (Future<ItemRequestSendDTO> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        // Then: все запросы созданы успешно
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().map(ItemRequestSendDTO::getId).collect(Collectors.toSet()))
                .hasSize(threadCount); // все ID уникальны
    }

   /* @Test
    void patchStatus_ShouldEnforceBusinessRulesForStatusTransitions() {
        // Given: запрос в статусе COMPLETED
        ItemRequestSendDTO createdRequest = createTestRequest("Test request", requesterId);

        ItemRequestReqDTO completeDto = new ItemRequestReqDTO();
        completeDto.setStatus(RequestStatus.COMPLETED);
        requestService.patchStatus(completeDto, createdRequest.getId(), requesterId);

        // Пытаемся изменить статус завершённого запроса
        ItemRequestReqDTO changeDto = new ItemRequestReqDTO();
        changeDto.setStatus(RequestStatus.PENDING);

        // When & Then: должен быть запрет на изменение статуса завершённого запроса
        assertThatThrownBy(() -> requestService.patchStatus(changeDto, createdRequest.getId(), requesterId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Запрос с id: 3 не существует или имеет неактивный статус");
    }*/

    @Test
    void getByIdWithItems_ShouldReturnRequestWithAllAssociatedItems() {
        // Given: запрос с несколькими вещами
        ItemRequestSendDTO createdRequest = createTestRequest("Request with multiple items", requesterId);
        addItemsToRequest(createdRequest.getId(), 3); // добавляем 3 вещи
        // When
        ItemRequestWithItemsDto result = requestService.getByIdWithItems(createdRequest.getId());

        // Then
        assertThat(result.getItems()).hasSize(3);
        assertThat(result.getId()).isEqualTo(createdRequest.getId());
        assertThat(result.getDescription()).isEqualTo("Request with multiple items");
    }


    @Test
    void getById_ShouldThrowNoSuchElementException_WhenZeroIdProvided() {
        // Given
        Long zeroId = 0L;

        // When & Then
        assertThatThrownBy(() -> requestService.getById(zeroId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не существует");
    }

    @Test
    void deleteById_ShouldThrowSecurityException_WhenNegativeUserIdProvided() {
        // Given
        ItemRequestSendDTO createdRequest = createTestRequest(TEST_DESCRIPTION, requesterId);
        Long negativeUserId = -1L;

        // When & Then
        assertThatThrownBy(() -> requestService.deleteById(createdRequest.getId(), negativeUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Попытка несанкционированного доступа");
    }

    @Test
    void deleteById_ShouldCleanupAssociatedData_WhenRequestIsDeleted() {
        // Given: создаём запрос с несколькими вещами
        ItemRequestSendDTO createdRequest = createTestRequest("Request with items to be deleted", requesterId);
        int itemCount = 2;
        addItemsToRequest(createdRequest.getId(), itemCount);

        // Проверяем, что вещи существуют до удаления
        List<Item> itemsBeforeDelete = itemRepository.findByRequestId(createdRequest.getId());
        assertThat(itemsBeforeDelete).hasSize(itemCount);

        // When: удаляем запрос
        requestService.deleteById(createdRequest.getId(), requesterId);

        // Then: проверяем, что запрос удалён
        assertThat(requestRepository.findById(createdRequest.getId())).isEmpty();

        // Проверяем, что связанные вещи также удалены или отвязаны
        List<Item> itemsAfterDelete = itemRepository.findByRequestId(createdRequest.getId());
        // В зависимости от бизнес‑логики: либо удалены, либо request = null
        assertThat(itemsAfterDelete).allMatch(item -> item.getRequest() == null);
    }

    // Метод для добавления вещей к запросу
    private void addItemsToRequest(Long requestId, int count) {
        User owner = userJPARepository.findById(requesterId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.setName("Test item " + i);
            item.setDescription("Description for test item " + i);
            item.setAvailable(true);
            item.setOwner(owner);
            item.setRequest(requestRepository.findById(requestId)
                    .orElseThrow(() -> new NoSuchElementException("Request not found")));
            itemRepository.save(item);
        }
    }

    // Поле для доступа к репозиторию вещей (внедрите через @Autowired)
    @Autowired
    private ItemJpaRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

}

