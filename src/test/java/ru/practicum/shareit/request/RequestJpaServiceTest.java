package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RequestJpaServiceTest {

    @Autowired
    private RequestJpaRepository requestRepository;

    @Autowired
    private UserJPARepository userJPARepository;

    private UserService userService;
    private RequestJpaService requestService;
    private UserMapper userMapper;

    private Long requesterId;
    private static final String TEST_DESCRIPTION = "Test request description";

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        requestRepository.deleteAll();
        userJPARepository.deleteAll();

        // Создаём и сохраняем пользователя напрямую через репозиторий
        User requester = new User();
        requester.setName("Requester User");
        requester.setEmail("requester@example.com");

        User savedRequester = userJPARepository.save(requester);
        requesterId = savedRequester.getId();

        // Инициализируем сервисы с реальными репозиториями
        userService = new UserJPAService(userJPARepository, userMapper);
        requestService = new RequestJpaService(requestRepository, userService);
    }

    @Test
    void create_ShouldSaveRequestWithAllValidFields_WhenDescriptionIsProvided() {
        // Given: создаём DTO с описанием
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription(TEST_DESCRIPTION);

        // When: создаём запрос через сервис
        ItemRequestSendDTO result = requestService.create(requestDto, requesterId);

        // Then: проверяем, что ID сгенерирован
        assertThat(result.getId()).isNotNull();

        // Проверяем, что описание совпадает с переданным
        assertThat(result.getDescription()).isEqualTo(TEST_DESCRIPTION);

        // Исправленная проверка: сравниваем ID пользователя
        assertThat(result.getRequester().getId()).isEqualTo(requesterId);

        // Проверяем, что статус установлен как PENDING по умолчанию
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);

        // Проверяем, что дата создания установлена и не null
        assertThat(result.getCreated()).isNotNull();

        // Дополнительно проверяем сохранённый объект в БД
        Optional<ru.practicum.shareit.request.ItemRequest> savedRequest =
                requestRepository.findById(result.getId());

        assertThat(savedRequest).isPresent();

        ru.practicum.shareit.request.ItemRequest actualRequest = savedRequest.get();

        // Проверяем все поля сущности в БД
        assertThat(actualRequest.getDescription()).isEqualTo(TEST_DESCRIPTION);
        assertThat(actualRequest.getRequester().getId()).isEqualTo(requesterId);
        assertThat(actualRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(actualRequest.getCreated()).isNotNull();
        assertThat(actualRequest.getCreated())
                .isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void create_ShouldThrowException_WhenUserDoesNotExist() {
        // Given: несуществующий ID пользователя
        Long nonExistentUserId = 999L;
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription("Another test description");

        // When & Then: ожидаем исключение при попытке создать запрос с несуществующим пользователем
        assertThatThrownBy(() -> requestService.create(requestDto, nonExistentUserId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_ShouldSaveRequest_WhenDescriptionIsEmpty() {
        // Given: DTO с пустым описанием
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription("");

        // When: создаём запрос
        ItemRequestSendDTO result = requestService.create(requestDto, requesterId);

        // Then: проверяем основные поля
        assertThat(result.getId()).isNotNull();
        assertThat(result.getDescription()).isEmpty();
        assertThat(result.getRequester().getId()).isEqualTo(requesterId);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getCreated()).isNotNull();
    }

    @Test
    void patchStatus_ShouldThrowNoSuchElementException_WhenRequestDoesNotExist() {
        // Given: несуществующий ID запроса и DTO с допустимым статусом
        Long nonExistentRequestId = 999L;
        Long validRequestorId = requesterId;
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setStatus(RequestStatus.COMPLETED);

        // When & Then: ожидаем исключение NoSuchElementException, когда запрос не найден
        assertThatThrownBy(() -> requestService.patchStatus(requestDto, nonExistentRequestId, validRequestorId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не существует или имеет неактивный статус");
    }

    @Test
    void patchStatus_ShouldThrowSecurityException_WhenUserHasNoAccess() {
        // Given: создаём запрос от одного пользователя
        ItemRequestReqDTO createDto = new ItemRequestReqDTO();
        createDto.setDescription("Original request");
        ItemRequestSendDTO createdRequest = requestService.create(createDto, requesterId);

        // Создаём другого пользователя, который попытается изменить запрос
        User unauthorizedUser = new User();
        unauthorizedUser.setName("Unauthorized User");
        unauthorizedUser.setEmail("unauthorized@example.com");
        User savedUnauthorizedUser = userJPARepository.save(unauthorizedUser);
        Long unauthorizedUserId = savedUnauthorizedUser.getId();

        // DTO с новым статусом
        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.COMPLETED);

        // When & Then: ожидаем SecurityException при попытке доступа без прав
        assertThatThrownBy(() -> requestService.patchStatus(patchDto, createdRequest.getId(), unauthorizedUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Попытка несанкционированного доступа");
    }

    @Test
    void patchStatus_ShouldThrowIllegalArgumentException_WhenInvalidStatusProvided() {
        // Given: создаём запрос
        ItemRequestReqDTO createDto = new ItemRequestReqDTO();
        createDto.setDescription("Test request for status validation");
        ItemRequestSendDTO createdRequest = requestService.create(createDto, requesterId);

        // Пытаемся установить недопустимый статус (PENDING)
        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.PENDING);

        // When & Then: ожидаем IllegalArgumentException при установке системного статуса
        assertThatThrownBy(() -> requestService.patchStatus(patchDto, createdRequest.getId(), requesterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь пытается установить системный статус");

        // Дополнительно проверяем, что статус не изменился в БД
        Optional<ru.practicum.shareit.request.ItemRequest> updatedRequest =
                requestRepository.findById(createdRequest.getId());
        assertThat(updatedRequest).isPresent();
        assertThat(updatedRequest.get().getStatus()).isEqualTo(RequestStatus.PENDING); // остался прежним
    }

    @Test
    void patchStatus_ShouldUpdateStatusSuccessfully_WhenValidStatusAndAccess() {
        // Given: создаём запрос
        ItemRequestReqDTO createDto = new ItemRequestReqDTO();
        createDto.setDescription("Test request for successful status update");
        ItemRequestSendDTO createdRequest = requestService.create(createDto, requesterId);

        // Подготавливаем DTO с допустимым новым статусом (например, REJECTED)
        ItemRequestReqDTO patchDto = new ItemRequestReqDTO();
        patchDto.setStatus(RequestStatus.COMPLETED);

        // When: обновляем статус
        ItemRequestSendDTO result = requestService.patchStatus(patchDto, createdRequest.getId(), requesterId);

        // Then: проверяем, что ответ содержит новый статус
        assertThat(result.getStatus()).isEqualTo(RequestStatus.COMPLETED);

        // Проверяем, что в БД статус также обновился
        Optional<ru.practicum.shareit.request.ItemRequest> updatedRequestInDb =
                requestRepository.findById(createdRequest.getId());
        assertThat(updatedRequestInDb).isPresent();
        assertThat(updatedRequestInDb.get().getStatus()).isEqualTo(RequestStatus.COMPLETED);

        // Дополнительно можно проверить, что другие поля не изменились
        assertThat(updatedRequestInDb.get().getDescription()).isEqualTo("Test request for successful status update");
        assertThat(updatedRequestInDb.get().getRequester().getId()).isEqualTo(requesterId);
    }

    @Test
    void getById_ShouldReturnRequest_WhenRequestExists() {
        // Given: создаём запрос
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription(TEST_DESCRIPTION);
        ItemRequestSendDTO createdRequest = requestService.create(requestDto, requesterId);

        // When: получаем запрос по ID
        ItemRequestSendDTO result = requestService.getById(createdRequest.getId());

        // Then: проверяем, что возвращённый объект соответствует созданному
        assertThat(result.getId()).isEqualTo(createdRequest.getId());
        assertThat(result.getDescription()).isEqualTo(TEST_DESCRIPTION);
        assertThat(result.getRequester().getId()).isEqualTo(requesterId);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getCreated()).isNotNull();
    }

    @Test
    void getById_ShouldThrowNoSuchElementException_WhenRequestDoesNotExist() {
        // Given: несуществующий ID запроса
        Long nonExistentRequestId = 999L;

        // When & Then: ожидаем исключение NoSuchElementException при попытке получить несуществующий запрос
        assertThatThrownBy(() -> requestService.getById(nonExistentRequestId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не существует");
    }

    @Test
    void deleteById_ShouldDeleteRequest_WhenRequestExistsAndUserHasAccess() {
        // Given: создаём запрос
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription(TEST_DESCRIPTION);
        ItemRequestSendDTO createdRequest = requestService.create(requestDto, requesterId);

        // When: удаляем запрос
        requestService.deleteById(createdRequest.getId(), requesterId);

        // Then: проверяем, что запрос больше не существует в БД
        assertThat(requestRepository.findById(createdRequest.getId())).isEmpty();
    }

    @Test
    void deleteById_ShouldThrowSecurityException_WhenUserHasNoAccess() {
        // Given: создаём запрос от одного пользователя
        ItemRequestReqDTO createDto = new ItemRequestReqDTO();
        createDto.setDescription("Original request");
        ItemRequestSendDTO createdRequest = requestService.create(createDto, requesterId);

        // Создаём другого пользователя, который попытается удалить запрос
        User unauthorizedUser = new User();
        unauthorizedUser.setName("Unauthorized User");
        unauthorizedUser.setEmail("unauthorized@example.com");
        User savedUnauthorizedUser = userJPARepository.save(unauthorizedUser);
        Long unauthorizedUserId = savedUnauthorizedUser.getId();

        // When & Then: ожидаем SecurityException при попытке удаления без прав
        assertThatThrownBy(() -> requestService.deleteById(createdRequest.getId(), unauthorizedUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Попытка несанкционированного доступа");

        // Дополнительно проверяем, что запрос остался в БД
        assertThat(requestRepository.findById(createdRequest.getId())).isPresent();
    }

    @Test
    void getAllByRequestorId_ShouldReturnAllRequests_WhenMultipleRequestsExist() {
        // Given: создаём два запроса от одного пользователя
        ItemRequestReqDTO firstDto = new ItemRequestReqDTO();
        firstDto.setDescription("First request description");
        ItemRequestSendDTO firstRequest = requestService.create(firstDto, requesterId);

        ItemRequestReqDTO secondDto = new ItemRequestReqDTO();
        secondDto.setDescription("Second request description");
        ItemRequestSendDTO secondRequest = requestService.create(secondDto, requesterId);

        // When: получаем все запросы пользователя
        List<ItemRequestSendDTO> requests = requestService.getAllByRequestorId(requesterId);

        // Then: проверяем, что вернулись оба запроса
        assertThat(requests).hasSize(2);
        assertThat(requests)
                .extracting("description")
                .containsExactlyInAnyOrder("First request description", "Second request description");
    }

    @Test
    void getAllByRequestorId_ShouldReturnOneRequest_AfterOneIsDeleted() {
        // Given: создаём два запроса от одного пользователя
        ItemRequestReqDTO firstDto = new ItemRequestReqDTO();
        firstDto.setDescription("First request description");
        ItemRequestSendDTO firstRequest = requestService.create(firstDto, requesterId);

        ItemRequestReqDTO secondDto = new ItemRequestReqDTO();
        secondDto.setDescription("Second request description");
        ItemRequestSendDTO secondRequest = requestService.create(secondDto, requesterId);

        // Удаляем один запрос
        requestService.deleteById(firstRequest.getId(), requesterId);

        // When: получаем оставшиеся запросы пользователя
        List<ItemRequestSendDTO> remainingRequests = requestService.getAllByRequestorId(requesterId);

        // Then: проверяем, что остался только один запрос
        assertThat(remainingRequests).hasSize(1);
        assertThat(remainingRequests.getFirst().getId()).isEqualTo(secondRequest.getId());
        assertThat(remainingRequests.getFirst().getDescription()).isEqualTo("Second request description");
    }

}
