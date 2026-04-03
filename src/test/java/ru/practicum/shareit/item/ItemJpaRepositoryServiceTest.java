    package ru.practicum.shareit.item;

    import jakarta.persistence.EntityNotFoundException;
    import jakarta.transaction.Transactional;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
    import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
    import org.springframework.context.annotation.Import;
    import ru.practicum.shareit.TestServiceConfiguration;
    import ru.practicum.shareit.booking.Booking;
    import ru.practicum.shareit.booking.dto.BookingReqDto;
    import ru.practicum.shareit.booking.dto.BookingSendDto;
    import ru.practicum.shareit.booking.repository.BookingJpaRepository;
    import ru.practicum.shareit.booking.service.BookingJpaService;
    import ru.practicum.shareit.common.enums.BookingStatus;
    import ru.practicum.shareit.common.enums.RequestStatus;
    import ru.practicum.shareit.item.dto.ItemReqDTO;
    import ru.practicum.shareit.item.dto.ItemSendDTO;
    import ru.practicum.shareit.item.repository.ItemJPARepository;
    import ru.practicum.shareit.item.service.ItemJPAService;
    import ru.practicum.shareit.request.ItemRequest;
    import ru.practicum.shareit.request.repository.RequestJpaRepository;
    import ru.practicum.shareit.request.service.RequestJpaService;
    import ru.practicum.shareit.user.User;
    import ru.practicum.shareit.user.dto.UserReqDTO;
    import ru.practicum.shareit.user.repository.UserJPARepository;
    import ru.practicum.shareit.user.service.UserJPAService;

    import java.time.LocalDateTime;
    import java.time.temporal.ChronoUnit;
    import java.util.*;

    import static org.assertj.core.api.Assertions.*;

    @DataJpaTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    @Import(TestServiceConfiguration.class)
    class ItemJpaRepositoryServiceTest {

        @Autowired
        private ItemJPARepository itemRepository;

        @Autowired
        private UserJPARepository userRepository;

        @Autowired
        private BookingJpaRepository bookingRepository;

        @Autowired
        private RequestJpaRepository requestRepository;

        @Autowired
        private UserJPAService userJPAService;

        @Autowired
        BookingJpaService bookingJpaService;

        @Autowired
        private RequestJpaService requestJpaService;

        @Autowired
        private ItemJPAService itemService;


        // Остальной код остаётся без изменений
        private Long ownerId;
        private ItemReqDTO dto;
        private Long requestId;

        private static final int MAX_DESCRIPTION_LENGTH = 100;

        @BeforeEach
        @Transactional
        void setUp() {
            itemRepository.deleteAll();
            requestRepository.deleteAll();
            userRepository.deleteAll();

            ownerId = createTestUser();
            requestId = createTestRequest(ownerId);
            dto = createItemReqDTO();
        }


        private Long createTestUser() {
            User user = new User();
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
            user.setName("Test User " + uniqueSuffix);
            user.setEmail("test-" + uniqueSuffix + "@user.com");
            User savedUser = userRepository.save(user);
            return savedUser.getId();
        }

        private Long createTestRequest(Long userId) {
            // Получаем пользователя по ID
            User requester = userRepository.findById(userId)
                    .orElseThrow(() -> new AssertionError("User with ID " + userId + " not found"));

            ItemRequest request = new ItemRequest();
            request.setDescription("Test request description");
            request.setRequester(requester);
            request.setCreated(LocalDateTime.now());
            request.setStatus(RequestStatus.PENDING);

            ItemRequest savedRequest = requestRepository.save(request);
            return savedRequest.getId();
        }

        private ItemReqDTO createItemReqDTO() {
            ItemReqDTO dto = new ItemReqDTO();
            dto.setName("Test Item");
            dto.setDescription("Valid description");
            dto.setRequestId(null);
            dto.setAvailable(true);
            return dto;
        }

        @Test
        void create_shouldCreateItemWithoutRequest() {
            // given
            dto.setRequestId(null);

            // when
            ItemSendDTO result = itemService.create(ownerId, dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(dto.getName());
            assertThat(result.getDescription()).isEqualTo(dto.getDescription());
            assertThat(result.getOwner().getId()).isEqualTo(ownerId);
            assertThat(result.getRequest()).isNull();

            // Проверяем, что в БД сохранилась вещь без запроса
            Item savedItem = itemRepository.findById(result.getId()).orElse(null);
            assertThat(savedItem).isNotNull();
            assertThat(savedItem.getRequest()).isNull();
        }

        @Test
        void create_shouldCreateItemWithRequestAndUpdateRequestStatus() {
            // given
            dto.setRequestId(requestId);

            // Получаем исходный запрос для проверки статуса
            ItemRequest originalRequest = requestRepository.findById(requestId)
                    .orElseThrow(() -> new AssertionError("Запрос не найден в БД"));
            assertThat(originalRequest.getStatus()).isEqualTo(RequestStatus.PENDING);

            // when
            ItemSendDTO result = itemService.create(ownerId, dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRequest().getId()).isEqualTo(requestId);

            // Проверяем обновление статуса запроса в БД
            ItemRequest updatedRequest = requestRepository.findById(requestId).orElse(null);
            assertThat(updatedRequest).isNotNull();
            assertThat(updatedRequest.getStatus()).isEqualTo(RequestStatus.RESPONDED);
        }

        @Test
        void create_shouldThrowExceptionWhenDescriptionTooLong() {
            // given
            String longDescription = "A".repeat(MAX_DESCRIPTION_LENGTH + 1);
            dto.setDescription(longDescription);

            // when & then
            assertThatThrownBy(() -> itemService.create(ownerId, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Описание не может превышать 100 символов");
        }

        @Test
        void create_shouldThrowExceptionWhenOwnerNotFound() {
            // given
            Long nonExistentOwnerId = 999L;

            // when & then
            assertThatThrownBy(() -> itemService.create(nonExistentOwnerId, dto))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void create_shouldThrowExceptionWhenRequestNotFound() {
            // given
            Long nonExistentRequestId = 999L;
            dto.setRequestId(nonExistentRequestId);

            // when & then
            assertThatThrownBy(() -> itemService.create(ownerId, dto))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Запрос с id: " + nonExistentRequestId + " не существует");
        }

        @Test
        void create_shouldThrowExceptionWhenRequestHasInactiveStatus() {
            // given
            // Создаём запрос с неактивным статусом
            ItemRequest inactiveRequest = new ItemRequest();
            inactiveRequest.setDescription("Inactive request");
            inactiveRequest.setStatus(RequestStatus.CANCELLED); // или другой неактивный статус
            inactiveRequest.setRequester(userRepository.findById(ownerId).orElse(null));
            inactiveRequest.setCreated(LocalDateTime.now());

            ItemRequest savedInactiveRequest = requestRepository.save(inactiveRequest);
            Long inactiveRequestId = savedInactiveRequest.getId();

            dto.setRequestId(inactiveRequestId);

            // when & then
            assertThatThrownBy(() -> itemService.create(ownerId, dto))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("не существует или имеет неактивный статус");
        }


        @Test
        void create_shouldReturnCorrectDtoFields() {
            // given
            dto.setName("Test Item Full");
            dto.setDescription("Full description");
            dto.setAvailable(true);
            dto.setRequestId(null);

            // when
            ItemSendDTO result = itemService.create(ownerId, dto);

            // then
            assertThat(result.getName()).isEqualTo(dto.getName());
            assertThat(result.getDescription()).isEqualTo(dto.getDescription());
            assertThat(result.getAvailable()).isEqualTo(dto.getAvailable());
            assertThat(result.getId()).isNotNull();
            assertThat(result.getOwner()).isNotNull();
        }

        @Test
        void update_shouldUpdateItemSuccessfully() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();

            ItemReqDTO updateDto = new ItemReqDTO();
            updateDto.setName("Updated Name");
            updateDto.setDescription("Updated Description");
            updateDto.setAvailable(false);

            // when
            ItemSendDTO updatedItem = itemService.update(itemId, ownerId, updateDto);

            // then
            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.getId()).isEqualTo(itemId);
            assertThat(updatedItem.getName()).isEqualTo("Updated Name");
            assertThat(updatedItem.getDescription()).isEqualTo("Updated Description");
            assertThat(updatedItem.getAvailable()).isFalse();

            // Проверяем в БД
            Item dbItem = itemRepository.findById(itemId).orElse(null);
            assertThat(dbItem).isNotNull();
            assertThat(dbItem.getName()).isEqualTo("Updated Name");
            assertThat(dbItem.getDescription()).isEqualTo("Updated Description");
            assertThat(dbItem.getAvailable()).isFalse();
        }

        @Test
        void update_shouldThrowExceptionWhenItemNotFound() {
            // given
            Long nonExistentItemId = 999L;
            ItemReqDTO updateDto = createItemReqDTO();

            // when & then
            assertThatThrownBy(() -> itemService.update(nonExistentItemId, ownerId, updateDto))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Вещь с ID:" + nonExistentItemId + " не существует");
        }


        @Test
        void update_shouldThrowExceptionWhenOwnerDoesNotMatch() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();

            // Создаём другого владельца с уникальным email
            User otherUser = new User();
            otherUser.setName("Other Test User");
            otherUser.setEmail("other-test@user.com"); // Уникальный email
            User savedOtherUser = userRepository.save(otherUser);
            Long otherOwnerId = savedOtherUser.getId();

            ItemReqDTO updateDto = createItemReqDTO();

            // when & then
            assertThatThrownBy(() -> itemService.update(itemId, otherOwnerId, updateDto))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void update_shouldThrowExceptionWhenNoFieldsToUpdate() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();
            ItemReqDTO emptyDto = new ItemReqDTO(); // Пустой DTO

            // when & then
            assertThatThrownBy(() -> itemService.update(itemId, ownerId, emptyDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("При обновлении хотя бы одно поле должно быть заполнено");
        }

        @Test
        void update_shouldValidateDescriptionLength() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();

            ItemReqDTO updateDto = new ItemReqDTO();
            updateDto.setDescription("A".repeat(MAX_DESCRIPTION_LENGTH + 1)); // Слишком длинное описание

            // when & then
            assertThatThrownBy(() -> itemService.update(itemId, ownerId, updateDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Описание не может превышать 100 символов");
        }

        @Test
        void getById_shouldReturnItemSuccessfully() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();

            // when
            ItemSendDTO foundItem = itemService.getById(itemId);

            // then
            assertThat(foundItem).isNotNull();
            assertThat(foundItem.getId()).isEqualTo(itemId);
            assertThat(foundItem.getName()).isEqualTo(dto.getName());
            assertThat(foundItem.getDescription()).isEqualTo(dto.getDescription());
        }

        @Test
        void getById_shouldThrowExceptionWhenItemNotFound() {
            // given
            Long nonExistentItemId = 999L;

            // when & then
            assertThatThrownBy(() -> itemService.getById(nonExistentItemId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Вещь с ID:" + nonExistentItemId + " не существует");
        }

        @Test
        void getOwnerItems_shouldReturnAllItemsForOwner() {
            // given
            // Создаём несколько вещей для одного владельца
            ItemReqDTO dto1 = createItemReqDTO();
            dto1.setName("Item 1");
            itemService.create(ownerId, dto1);

            ItemReqDTO dto2 = createItemReqDTO();
            dto2.setName("Item 2");
            itemService.create(ownerId, dto2);

            // Создаём вещь для другого владельца (не должна попасть в результат)
            Long otherOwnerId = createTestUser();
            ItemReqDTO otherDto = createItemReqDTO();
            otherDto.setName("Other Owner's Item");
            itemService.create(otherOwnerId, otherDto);

            // when
            List<ItemSendDTO> ownerItems = itemService.getOwnerItems(ownerId);

            // then
            assertThat(ownerItems).hasSize(2);
            assertThat(ownerItems)
                    .extracting("name")
                    .containsExactlyInAnyOrder("Item 1", "Item 2");

            // Проверяем, что вещи другого владельца нет в результате
            assertThat(ownerItems)
                    .extracting("name")
                    .doesNotContain("Other Owner's Item");
        }

        @Test
        void getOwnerItems_shouldReturnEmptyListWhenOwnerHasNoItems() {
            // given
            Long newOwnerId = createTestUser();

            // when
            List<ItemSendDTO> items = itemService.getOwnerItems(newOwnerId);

            // then
            assertThat(items).isEmpty();
        }

        @Test
        void findItemsByRequestIdForRequester_shouldReturnItemsForRequest() {
            // given
            // Создаём запрос и несколько вещей к нему
            ItemRequest request = new ItemRequest();
            request.setDescription("Test search request");
            request.setRequester(userRepository.findById(ownerId).orElse(null));
            request.setCreated(LocalDateTime.now());
            request.setStatus(RequestStatus.PENDING);
            ItemRequest savedRequest = requestRepository.save(request);

            // Создаём вещи, связанные с запросом
            ItemReqDTO itemDto1 = createItemReqDTO();
            itemDto1.setRequestId(savedRequest.getId());
            itemService.create(ownerId, itemDto1);

            ItemReqDTO itemDto2 = createItemReqDTO();
            itemDto2.setName("Second Item for Request");
            itemDto2.setRequestId(savedRequest.getId());
            itemService.create(ownerId, itemDto2);

            // when
            Collection<ItemSendDTO> items = itemService.findItemsByRequestIdForRequester(
                    savedRequest.getId(), ownerId);

            // then
            assertThat(items).hasSize(2);
            assertThat(items)
                    .extracting("name")
                    .contains("Test Item", "Second Item for Request");
        }

        @Test
        void findItemsByRequestIdForRequester_shouldThrowExceptionWhenRequesterIsNotOwner() {
            // given
            // Создаём запрос от одного пользователя
            Long requesterId = createTestUser();
            ItemRequest request = new ItemRequest();
            request.setDescription("Private request");
            request.setRequester(userRepository.findById(requesterId).orElse(null));
            request.setCreated(LocalDateTime.now());
            request.setStatus(RequestStatus.PENDING);
            ItemRequest savedRequest = requestRepository.save(request);

            // Другой пользователь пытается получить вещи
            Long unauthorizedUserId = createTestUser();

            // when & then
            assertThatThrownBy(() -> itemService.findItemsByRequestIdForRequester(
                    savedRequest.getId(), unauthorizedUserId))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void findItemsByRequestIdForRequester_shouldReturnEmptyCollectionWhenNoItemsForRequest() {
            // given
            // Создаём запрос без связанных вещей
            ItemRequest request = new ItemRequest();
            request.setDescription("Empty request");
            request.setRequester(userRepository.findById(ownerId).orElse(null));
            request.setCreated(LocalDateTime.now());
            request.setStatus(RequestStatus.PENDING);
            ItemRequest savedRequest = requestRepository.save(request);

            // when
            Collection<ItemSendDTO> items = itemService.findItemsByRequestIdForRequester(
                    savedRequest.getId(), ownerId);

            // then
            assertThat(items).isEmpty();
        }

        @Test
        void findItemsByRequestIdForRequester_shouldThrowExceptionWhenRequestNotFound() {
            // given
            Long nonExistentRequestId = 999L;

            // when & then
            assertThatThrownBy(() -> itemService.findItemsByRequestIdForRequester(
                    nonExistentRequestId, ownerId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Запрос с id: " + nonExistentRequestId + " не существует");
        }

        @Test
        void deleteByIdForOwner_shouldDeleteItemSuccessfully() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();

            // Проверяем, что вещь существует до удаления
            assertThat(itemRepository.findById(itemId)).isPresent();

            // when
            itemService.deleteByIdForOwner(ownerId, itemId);

            // then
            // Проверяем, что вещь удалена из БД
            assertThat(itemRepository.findById(itemId)).isEmpty();
        }

        @Test
        void deleteByIdForOwner_shouldThrowExceptionWhenItemNotFound() {
            // given
            Long nonExistentItemId = 999L;

            // when & then
            assertThatThrownBy(() -> itemService.deleteByIdForOwner(ownerId, nonExistentItemId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Вещь с ID:" + nonExistentItemId + " не существует");
        }

        @Test
        void deleteByIdForOwner_shouldThrowExceptionWhenOwnerDoesNotMatch() {
            // given
            ItemSendDTO createdItem = itemService.create(ownerId, dto);
            Long itemId = createdItem.getId();
            Long otherOwnerId = createTestUser(); // Создаём другого владельца

            // when & then
            assertThatThrownBy(() -> itemService.deleteByIdForOwner(otherOwnerId, itemId))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void search_shouldReturnEmptyListWhenTextIsNull() {
            // given
            String searchText = null;

            // when
            List<ItemSendDTO> results = itemService.search(searchText);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        void search_shouldReturnEmptyListWhenTextIsEmpty() {
            // given
            String searchText = "";

            // when
            List<ItemSendDTO> results = itemService.search(searchText);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        void search_shouldReturnEmptyListWhenNoMatchesFound() {
            // given
            String searchText = "nonexistent item";

            // Создаём вещь с другим названием
            ItemReqDTO dto1 = createItemReqDTO();
            dto1.setName("Completely different item");
            itemService.create(ownerId, dto1);

            // when
            List<ItemSendDTO> results = itemService.search(searchText);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        void search_shouldFindItemsByName() {
            // given
            // Создаём вещи с разными названиями
            ItemReqDTO dto1 = createItemReqDTO();
            dto1.setName("Hammer");
            itemService.create(ownerId, dto1);

            ItemReqDTO dto2 = createItemReqDTO();
            dto2.setName("Screwdriver");
            itemService.create(ownerId, dto2);

            String searchText = "ham"; // Поиск по части названия

            // when
            List<ItemSendDTO> results = itemService.search(searchText);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).containsIgnoringCase("Hammer");
        }

        @Test
        void search_shouldFindItemsByDescription() {
            // given
            // Создаём вещи с разными описаниями
            ItemReqDTO dto1 = createItemReqDTO();
            dto1.setDescription("Useful tool for hammering");
            itemService.create(ownerId, dto1);

            ItemReqDTO dto2 = createItemReqDTO();
            dto2.setDescription("Tool for turning screws");
            itemService.create(ownerId, dto2);

            String searchText = "hammer"; // Поиск по части описания

            // when
            List<ItemSendDTO> results = itemService.search(searchText);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).containsIgnoringCase("hammering");
        }

        @Test
        void search_shouldBeCaseInsensitive() {
            // given
            ItemReqDTO dto1 = createItemReqDTO();
            dto1.setName("Wooden Chair");
            dto1.setDescription("A comfortable chair made of wood");
            itemService.create(ownerId, dto1);

            // when ищем в разных регистрах
            List<ItemSendDTO> upperCaseResults = itemService.search("WOOD");
            List<ItemSendDTO> lowerCaseResults = itemService.search("wood");

            // then
            assertThat(upperCaseResults).hasSize(1);
            assertThat(lowerCaseResults).hasSize(1);
            assertThat(upperCaseResults.getFirst().getId()).isEqualTo(lowerCaseResults.getFirst().getId());
        }

        @Test
        void getOwnerItems_shouldReturnItemsWithCorrectBookingDates() {
            // given
            // Создаём владельца
            Long ownerId = createTestUser();
            Long bookerId = userJPAService.create(new UserReqDTO("nnn", "dsf@mail.en")).getId();

            // Создаём две вещи для владельца
            ItemReqDTO itemDto1 = createItemReqDTO();
            itemDto1.setName("Item with Past Booking");
            ItemSendDTO item1 = itemService.create(ownerId, itemDto1);

            ItemReqDTO itemDto2 = createItemReqDTO();
            itemDto2.setName("Item with Future Booking");
            ItemSendDTO item2 = itemService.create(ownerId, itemDto2);

            // Текущее время
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

            // Создаём прошлое бронирование для первой вещи (завершённое)
            BookingReqDto pastBookingDto = new BookingReqDto();
            pastBookingDto.setItemId(item1.getId());
            pastBookingDto.setStart(LocalDateTime.now().plusSeconds(1));
            pastBookingDto.setEnd(LocalDateTime.now().plusSeconds(2));

            BookingSendDto pastDto = bookingJpaService.create(pastBookingDto, bookerId); // предполагаем, что есть доступ к bookingService

            // Создаём будущее бронирование для второй вещи
            BookingReqDto futureBookingDto = new BookingReqDto();
            futureBookingDto.setItemId(item2.getId());
            futureBookingDto.setStart(now.plusDays(2));
            futureBookingDto.setEnd(now.plusDays(4));

            BookingSendDto futureDto = bookingJpaService.create(futureBookingDto, bookerId);
            System.out.println("ПРошлое ДТО ____________________"+bookingJpaService.findByIdOrThrowInternal(pastDto.getId()));
            System.out.println("Будущее ДТО ____________________"+bookingJpaService.findByIdOrThrowInternal(futureDto.getId()));
            // when
            List<ItemSendDTO> ownerItems = itemService.getOwnerItems(ownerId);

            // then
            assertThat(ownerItems).hasSize(2);

            // Находим вещи по имени для проверки
            Optional<ItemSendDTO> pastItemOpt = ownerItems.stream()
                    .filter(item -> item.getName().equals("Item with Past Booking"))
                    .findFirst();
            Optional<ItemSendDTO> futureItemOpt = ownerItems.stream()
                    .filter(item -> item.getName().equals("Item with Future Booking"))
                    .findFirst();

            assertThat(pastItemOpt).isPresent();
            assertThat(futureItemOpt).isPresent();

            ItemSendDTO pastItem = pastItemOpt.get();
            ItemSendDTO futureItem = futureItemOpt.get();
            System.out.println(pastItem);
            System.out.println(futureItem);
            // Проверяем lastBookingDate для вещи с прошлым бронированием

            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
          //  System.out.println("чтото есть"+itemService.find(item1.getId()));
            //System.out.println("чтото есть"+itemService.find(item2.getId()));
            // Проверяем nextBookingDate для вещи с будущим бронированием
            assertThat(futureItem.getNextBookingDate())
                    .isEqualTo(now.plusDays(2).truncatedTo(ChronoUnit.SECONDS));
            assertThat(futureItem.getLastBookingDate()).isNull();
        }

    }
