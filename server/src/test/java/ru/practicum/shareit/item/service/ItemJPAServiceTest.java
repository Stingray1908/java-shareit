package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentMapper;
import ru.practicum.shareit.item.comment.repository.CommentJpaRepository;
import ru.practicum.shareit.item.comment.service.CommentJpaService;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJpaRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserJpaService;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ItemJPAServiceTest {

    @Mock
    private ItemJpaRepository itemRepository;

    @Mock
    private CommentJpaService commentService; // добавляем этот mock

    private final List<String> validStatuses = List.of(
            "APPROVED",
            "COMPLETED"
    );

    @Mock
    private UserJpaService userService;

    @Mock
    private RequestJpaService requestService;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private CommentJpaRepository commentRepository;

    @Mock
    private BookingJpaRepository bookingRepository;

    @InjectMocks
    private ItemJPAService itemService;

    private User owner;
    private Item item;
    private ItemReqDTO itemReqDTO;
    private ItemSendDTO itemSendDTO;
    private ItemRequest itemRequest;
    private Comment comment;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        owner = new User();
        owner.setId(1L);
        owner.setName("Test Owner");
        owner.setEmail("owner@test.com");

        item = new Item();
        item.setId(1L);
        item.setName("Test Item");
        item.setDescription("Valid description");
        item.setAvailable(true);
        item.setOwner(owner);

        itemReqDTO = new ItemReqDTO();
        itemReqDTO.setName("Updated Item");
        itemReqDTO.setDescription("Updated description");
        itemReqDTO.setAvailable(false);

        itemSendDTO = new ItemSendDTO();
        itemSendDTO.setId(1L);
        itemSendDTO.setName("Test Item");
        itemSendDTO.setDescription("Valid description");
        itemSendDTO.setAvailable(true);

        itemRequest = new ItemRequest();
        itemRequest.setId(1L);
        itemRequest.setDescription("Test request");
        itemRequest.setRequester(owner);
        itemRequest.setStatus(RequestStatus.PENDING);

        comment = new Comment();
        comment.setId(1L);
        comment.setText("Great item!");
        comment.setBooker(owner);
        comment.setItem(item);
        comment.setCreatedAt(LocalDateTime.now());

        now = LocalDateTime.now();
    }

    @Test
    void create_shouldCreateItemSuccessfully() {
        // Given
        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(owner);
        when(itemMapper.toEntity(any(ItemReqDTO.class))).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(itemMapper.toSendDto(any(Item.class))).thenReturn(itemSendDTO);

        // When
        ItemSendDTO result = itemService.create(owner.getId(), itemReqDTO);

        // Then
        assertThat(result).isEqualTo(itemSendDTO);
        verify(userService, times(1)).getByIdOrThrowInternal(owner.getId());
        verify(itemMapper, times(1)).toEntity(itemReqDTO);
        verify(itemRepository, times(1)).save(item);
    }

    @Test
    void create_shouldUpdateRequestStatusWhenRequestIdProvided() {
        // Given
        itemReqDTO.setRequestId(1L);

        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(owner);
        when(requestService.findActiveRequestByIdOrThrowInternal(anyLong()))
                .thenReturn(itemRequest);
        when(itemMapper.toEntity(any(ItemReqDTO.class))).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(itemMapper.toSendDto(any(Item.class))).thenReturn(itemSendDTO);

        // Добавляем ожидание для requestMapper
        ItemRequestSendDTO expectedRequestDto = new ItemRequestSendDTO();
        expectedRequestDto.setId(1L);
        expectedRequestDto.setDescription("Test request");
        expectedRequestDto.setStatus(RequestStatus.RESPONDED);
        when(requestMapper.toSendDto(any(ItemRequest.class))).thenReturn(expectedRequestDto);

        // When
        ItemSendDTO result = itemService.create(owner.getId(), itemReqDTO);

        // Then
        assertThat(itemRequest.getStatus()).isEqualTo(RequestStatus.RESPONDED);

        verify(requestService, times(1))
                .findActiveRequestByIdOrThrowInternal(itemReqDTO.getRequestId());

        // Проверяем, что requestMapper был вызван для преобразования обновлённого запроса
        verify(requestMapper, times(1)).toSendDto(itemRequest);

        // Убеждаемся, что результат mapper соответствует ожиданиям
        assertThat(expectedRequestDto.getStatus()).isEqualTo(RequestStatus.RESPONDED);
    }


    @Test
    void getById_shouldThrowExceptionWhenItemNotFound() {
        // Given
        when(itemRepository.findItemWithBookingDatesAndCommentsById(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> itemService.getById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Вещь с ID:999 не существует");
    }

    @Test
    void findItemsByRequestIdForRequester_shouldReturnItems() {
        // Given
        List<Item> itemsList = Collections.singletonList(item);
        when(requestService.findRequestByIdOrThrowInternal(anyLong()))
                .thenReturn(itemRequest);
        when(itemRepository.findByRequestId(anyLong())).thenReturn(itemsList);
        when(itemMapper.toSendDto(any(Item.class))).thenReturn(itemSendDTO);

        // When
        Collection<ItemSendDTO> result = itemService
                .findItemsByRequestIdForRequester(itemRequest.getId(), owner.getId());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(itemSendDTO);
        verify(requestService, times(1))
                .findRequestByIdOrThrowInternal(itemRequest.getId());
        verify(itemRepository, times(1)).findByRequestId(itemRequest.getId());
    }

    @Test
    void findItemsByRequestIdForRequester_shouldThrowExceptionWhenOwnerMismatch() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        itemRequest.setRequester(otherUser);

        when(requestService.findRequestByIdOrThrowInternal(anyLong()))
                .thenReturn(itemRequest);

        // When & Then
        assertThatThrownBy(() -> itemService
                .findItemsByRequestIdForRequester(itemRequest.getId(), owner.getId()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void search_shouldReturnEmptyListWhenTextIsNull() {
        // When
        List<ItemSendDTO> results = itemService.search(null);

        // Then
        assertThat(results).isEmpty();
        verify(itemRepository, never()).searchItems(anyString());
    }

    @Test
    void search_shouldReturnEmptyListWhenTextIsEmpty() {
        // When
        List<ItemSendDTO> results = itemService.search("");

        // Then
        assertThat(results).isEmpty();
        verify(itemRepository, never()).searchItems(anyString());
    }

    @Test
    void search_shouldFindItemsByName() {
        // Given
        List<Item> foundItems = Collections.singletonList(item);
        when(itemRepository.searchItems(anyString())).thenReturn(foundItems);
        when(itemMapper.toSendDto(any(Item.class))).thenReturn(itemSendDTO);

        // When
        List<ItemSendDTO> results = itemService.search("test");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(itemSendDTO);
        verify(itemRepository, times(1)).searchItems("test");
    }

    @Test
    void update_shouldUpdateItemSuccessfully() {
        // Given
        ItemReqDTO updateDto = new ItemReqDTO();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated Description");
        updateDto.setAvailable(false);

        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(owner);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(itemMapper.toSendDto(any(Item.class))).thenReturn(itemSendDTO);

        // When
        ItemSendDTO result = itemService.update(item.getId(), owner.getId(), updateDto);

        // Then
        assertThat(result).isEqualTo(itemSendDTO);
        verify(itemRepository, times(1)).findById(item.getId());
        verify(itemRepository, times(1)).save(item);
        assertThat(item.getName()).isEqualTo("Updated Name");
        assertThat(item.getDescription()).isEqualTo("Updated Description");
        assertThat(item.getAvailable()).isFalse();
    }

    @Test
    void update_shouldThrowExceptionWhenNoFieldsToUpdate() {
        // Given
        ItemReqDTO emptyDto = new ItemReqDTO(); // все поля null

        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(owner);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        // When & Then
        assertThatThrownBy(() -> itemService.update(item.getId(), owner.getId(), emptyDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("При обновлении хотя бы одно поле должно быть заполнено");
    }

    @Test
    void getOwnerItems_shouldReturnItemsWithBookingDates() {
        // Given
        List<Object[]> results = Collections.singletonList(new Object[]{item, now.minusDays(1), now.plusDays(1)});

        when(itemRepository.findItemsWithBookingDatesOnlyByOwnerId(anyLong(), eq(validStatuses)))
                .thenReturn(results);
        when(itemMapper.toSendDto(any(Item.class))).thenReturn(itemSendDTO);

        // When
        List<ItemSendDTO> items = itemService.getOwnerItems(owner.getId());

        // Then
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEqualTo(itemSendDTO);
        verify(itemRepository, times(1)).findItemsWithBookingDatesOnlyByOwnerId(owner.getId(), validStatuses);
    }


    @Test
    void deleteByIdForOwner_shouldDeleteItemSuccessfully() {
        // Given
        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(owner);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        // When
        itemService.deleteByIdForOwner(owner.getId(), item.getId());

        // Then
        verify(itemRepository, times(1)).deleteById(item.getId());
    }

    @Test
    void deleteByIdForOwner_shouldThrowExceptionWhenOwnerMismatch() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);

        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(otherUser);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        // When & Then
        assertThatThrownBy(() -> itemService.deleteByIdForOwner(otherUser.getId(), item.getId()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void update_shouldThrowExceptionWhenDescriptionTooLong() {
        // Given
        ItemReqDTO updateDto = new ItemReqDTO();
        updateDto.setDescription("A".repeat(101)); // 101 символ

        when(userService.getByIdOrThrowInternal(anyLong())).thenReturn(owner);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        // When & Then
        assertThatThrownBy(() -> itemService.update(item.getId(), owner.getId(), updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Описание не может превышать 100 символов");
    }
}
