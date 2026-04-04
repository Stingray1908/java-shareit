package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserJPARepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
class   RequestJpaRepositoryServiceTest {

    @Autowired
    private RequestJpaRepository requestRepository;

    @Autowired
    private UserJPARepository userJPARepository;

    @Autowired
    private RequestJpaService requestService;

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
    void create_ShouldSaveRequest_WhenDescriptionIsEmpty() {
        // Given
        ItemRequestReqDTO requestDto = createRequestDto("");

        // When
        ItemRequestSendDTO result = requestService.create(requestDto, requesterId);

        // Then
        assertRequestFields(result, "", requesterId, RequestStatus.PENDING);
        assertSavedRequestInDb(result.getId(), "", requesterId, RequestStatus.PENDING);
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

    /*@Test
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
    }*/

    /*@Test
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
    }*/

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
}

