package ru.practicum.shareit.request.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.repository.ItemJpaRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserJpaRepository;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
class RequestJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private RequestJpaRepository requestRepository;
    @Autowired
    private ItemJpaRepository itemRepository;

    @AfterEach
    void tearDown() {
        itemRepository.deleteAll();
        requestRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Вспомогательные методы
    private User saveUser(String name) {
        return userRepository.save(new User(name, name + "@email.com"));
    }

    private ItemRequest saveRequest(User requester) {
        var request = new ItemRequest();
        request.setDescription("Нужен ноутбук для работы на неделю");
        request.setCreated(LocalDateTime.now());
        request.setStatus(RequestStatus.PENDING);
        request.setRequester(requester);
        return requestRepository.save(request);
    }

    private Item saveItem(User owner, ItemRequest request, boolean available) {
        var item = new Item();
        item.setName("Ноутбук Lenovo ThinkPad");
        item.setDescription("Ноутбук для работы, 16GB RAM, i7");
        item.setAvailable(available);
        item.setOwner(owner);
        item.setRequest(request);
        return itemRepository.save(item);
    }

    @Test
    void shouldCreateRequest() {
        // GIVEN
        var requester = saveUser("Requester");

        // WHEN
        var savedRequest = saveRequest(requester);

        // THEN
        assertThat(savedRequest.getDescription())
                .isEqualTo("Нужен ноутбук для работы на неделю");
        assertThat(savedRequest.getStatus())
                .isEqualTo(RequestStatus.PENDING);
        assertThat(savedRequest.getId())
                .isNotNull();
        assertThat(savedRequest.getRequester())
                .isEqualTo(requester);
        assertThat(savedRequest.getCreated())
                .isNotNull();
    }

    @Test
    void shouldGetMyRequestsWithItems() {
        // GIVEN: 2 пользователя, 2 запроса, 2 вещи в ответ
        var requester = saveUser("Requester");
        var owner = saveUser("Owner");

        var request1 = saveRequest(requester);
        var request2 = saveRequest(requester);

        saveItem(owner, request1, true);
        saveItem(owner, request2, false);

        entityManager.flush();
        entityManager.clear();

        // WHEN
        var requests = requestRepository.findAllByRequesterIdWithItems(requester.getId());

        // THEN: 2 запроса, отсортированы по дате (новые сначала)
        assertThat(requests).hasSize(2)
                .isSortedAccordingTo((r1, r2) -> r2.getCreated().compareTo(r1.getCreated()));

        // Проверяем первый запрос
        var firstRequest = requests.get(0);
        assertThat(firstRequest.getItems()).hasSize(1);
        assertThat(firstRequest.getItems().get(0))
                .hasFieldOrPropertyWithValue("name", "Ноутбук Lenovo ThinkPad")
                .extracting(item -> item.getOwner().getId())
                .isEqualTo(owner.getId());
    }

    @Test
    void shouldGetRequestWithItemsById() {
        // GIVEN: запрос с 2 вещами от разных владельцев
        var requester = saveUser("Requester");
        var owner1 = saveUser("Owner1");
        var owner2 = saveUser("Owner2");

        var request = saveRequest(requester);

        saveItem(owner1, request, true);
        saveItem(owner2, request, false);

        entityManager.flush();
        entityManager.clear();

        // WHEN
        var foundRequest = requestRepository.findByIdWithItems(request.getId());

        // THEN
        assertThat(foundRequest).isPresent();
        var result = foundRequest.get();

        assertThat(result)
                .hasFieldOrPropertyWithValue("id", request.getId())
                .hasFieldOrPropertyWithValue("description", "Нужен ноутбук для работы на неделю");

        // 2 вещи от разных владельцев
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems())
                .extracting(item -> item.getOwner().getId())
                .containsExactlyInAnyOrder(owner1.getId(), owner2.getId());
    }

    @Test
    void shouldFindRequestById() {
        // GIVEN
        var requester = saveUser("Requester");
        var savedRequest = saveRequest(requester);

        entityManager.flush();
        entityManager.clear();

        // WHEN
        var foundRequest = requestRepository.findById(savedRequest.getId());

        // THEN
        assertThat(foundRequest).isPresent();
        var result = foundRequest.get();
        assertThat(result.getId()).isEqualTo(savedRequest.getId());
        assertThat(result.getDescription()).isEqualTo("Нужен ноутбук для работы на неделю");
        assertThat(result.getRequester().getId()).isEqualTo(requester.getId());
    }

    @Test
    void shouldReturnEmptyOptionalWhenRequestNotFound() {
        // WHEN
        var foundRequest = requestRepository.findById(999L);

        // THEN
        assertThat(foundRequest).isEmpty();
    }

    @Test
    void shouldFindActiveRequestById() {
        // GIVEN
        var requester = saveUser("Requester");
        var savedRequest = saveRequest(requester); // статус PENDING

        entityManager.flush();
        entityManager.clear();

        // WHEN
        var foundRequest = requestRepository.findByIdWithActiveStatus(savedRequest.getId());

        // THEN
        assertThat(foundRequest).isPresent();
        var result = foundRequest.get();
        assertThat(result.getId()).isEqualTo(savedRequest.getId());
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void shouldNotFindInactiveRequestById() {
        // GIVEN
        var requester = saveUser("Requester");
        var request = new ItemRequest();
        request.setDescription("Запрос с неактивным статусом");
        request.setCreated(LocalDateTime.now());
        request.setStatus(RequestStatus.CANCELLED); // неактивный статус
        request.setRequester(requester);
        var savedRequest = requestRepository.save(request);

        entityManager.flush();
        entityManager.clear();

        // WHEN
        var foundRequest = requestRepository.findByIdWithActiveStatus(savedRequest.getId());

        // THEN
        assertThat(foundRequest).isEmpty();
    }

    @Test
    void shouldReturnEmptyOptionalWhenActiveRequestNotFound() {
        // WHEN
        var foundRequest = requestRepository.findByIdWithActiveStatus(999L);

        // THEN
        assertThat(foundRequest).isEmpty();
    }

    @Test
    void shouldFindAllRequestsByRequesterId() {
        // GIVEN
        var requester = saveUser("Requester");
        var otherUser = saveUser("OtherUser");

        var request1 = saveRequest(requester);
        var request2 = saveRequest(requester);
        saveRequest(otherUser); // запрос другого пользователя

        entityManager.flush();
        entityManager.clear();

        // WHEN
        var requests = requestRepository.findAllByRequesterId(requester.getId());

        // THEN
        assertThat(requests).hasSize(2);
        assertThat(requests)
                .extracting("description")
                .containsExactly(
                        "Нужен ноутбук для работы на неделю",
                        "Нужен ноутбук для работы на неделю"
                );
        assertThat(requests)
                .extracting(r -> r.getRequester().getId())
                .containsOnly(requester.getId());
    }

    @Test
    void shouldReturnEmptyListWhenNoRequestsForUser() {
        // GIVEN
        var user = saveUser("User");

        // WHEN
        var requests = requestRepository.findAllByRequesterId(user.getId());

        // THEN
        assertThat(requests).isEmpty();
    }

    @Test
    void shouldDeleteRequestById() {
        // GIVEN
        var requester = saveUser("Requester");
        var savedRequest = saveRequest(requester);

        entityManager.flush();
        entityManager.clear();

        // WHEN
        requestRepository.deleteById(savedRequest.getId());

        // THEN: проверяем, что запрос больше не находится
        var foundAfterDelete = requestRepository.findById(savedRequest.getId());
        assertThat(foundAfterDelete).isEmpty();

        // Дополнительно проверяем, что другие запросы не затронуты
        var anotherRequest = saveRequest(requester);
        var foundOther = requestRepository.findById(anotherRequest.getId());
        assertThat(foundOther).isPresent();
    }


    @Test
    void shouldNotThrowExceptionWhenDeletingNonExistingId() {
        // WHEN: пытаемся удалить несуществующий ID
        assertDoesNotThrow(() -> requestRepository.deleteById(999L));

        // THEN: тест проходит, если не было выброшено исключение
    }
}
