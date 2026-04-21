package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentMapper;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;
import ru.practicum.shareit.item.comment.repository.CommentJpaRepository;
import ru.practicum.shareit.item.comment.service.CommentJpaService;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJpaRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserJpaService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service("ItemJPAService")
public class ItemJPAService implements ItemService {

    private final ItemJpaRepository itemRepository;

    private final UserJpaService userService;
    private final RequestJpaService requestService;
    private final CommentJpaService commentService;

    private final ItemMapper itemMapper;
    private final RequestMapper requestMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    private final CommentJpaRepository commentRepository;
    private final BookingJpaRepository bookingRepository;

    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private final List<String> validStatuses = List.of(
            BookingStatus.APPROVED.name(),
            BookingStatus.COMPLETED.name());

    @Transactional
    @Override
    public ItemSendDTO create(Long ownerId, ItemReqDTO dto) {
        User owner = findUserOrThrow(ownerId);
        Item item = itemMapper.toEntity(dto);

        validateDescriptionLength(item.getDescription());

        Long requestId = dto.getRequestId();
        if (requestId != null) {
            ItemRequest request = requestService.findActiveRequestByIdOrThrowInternal(requestId);

            // меняем статус запроса только если текущий "PENDING" (в ожидании)
            if (request.getStatus().equals(RequestStatus.PENDING)) {
                request.setStatus(RequestStatus.RESPONDED);
                // тут автоматическое обновление request в БД
            }
            item.setRequest(request);
        }
        item.setOwner(owner);

        return toSendDTO(itemRepository.save(item));
    }

    @Override
    @Transactional
    public CommentSendDto addComment(Long userId, Long itemId, CommentReqDto dto) {
        User booker = userService.getByIdOrThrowInternal(userId);
        Item item = getByIdOrThrowInternal(itemId);

        // Проверка: пользователь действительно брал вещь в аренду
        if (!hasUserBookedItem(userId, itemId)) {
            throw new IllegalArgumentException("Пользователь не брал эту вещь в аренду, поэтому не может оставить отзыв");
        }

        // Создаём новый комментарий
        Comment comment = new Comment();
        comment.setBooker(booker);
        comment.setItem(item);
        comment.setText(dto.getText());
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        // Сохраняем в БД
        Comment savedComment = commentService.saveInternal(comment);

        // Добавляем комментарий в коллекцию вещей для автоматической загрузки
        item.getComments().add(savedComment);

        return commentMapper.toSendDto(savedComment);
    }

    /**
     * Проверяет, брал ли пользователь вещь в аренду (хотя бы одно завершённое бронирование в прошлом)
     */
    @Transactional(readOnly = true)
    private boolean hasUserBookedItem(Long userId, Long itemId) {
        LocalDateTime now = LocalDateTime.now();

        return bookingRepository.existsPastBooking(
                userId,
                itemId,
                validStatuses,
                now
        );
    }

    /**
     * Универсальный метод преобразования Item в ItemSendDTO
     *
     * @param result      массив объектов из запроса (Item, lastBooking, nextBooking)
     * @param hasDates    нужно ли устанавливать даты бронирований
     * @param hasComments нужно ли загружать комментарии
     */
    private ItemSendDTO toSendDTO(Object[] result, boolean hasDates, boolean hasComments) {
        if (result == null || result.length == 0) {
            throw new IllegalArgumentException("Пустой результат запроса");
        }

        Item item = (Item) result[0];
        if (item == null) {
            throw new NoSuchElementException("Предмет не найден в результате запроса");
        }

        LocalDateTime lastBooking = null;
        LocalDateTime nextBooking = null;

        if (hasDates) {
            lastBooking = result.length > 1 ? (LocalDateTime) result[1] : null;
            nextBooking = result.length > 2 ? (LocalDateTime) result[2] : null;
        }

        ItemSendDTO dto = itemMapper.toSendDto(item);
        dto.setLastBooking(lastBooking);
        dto.setNextBooking(nextBooking);

        // Комментарии загружаются только если hasComments = true
        if (hasComments && item.getComments() != null && !item.getComments().isEmpty()) {
            dto.setComments(item.getComments().stream()
                    .map(commentMapper::toSendDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setComments(Collections.emptyList());
        }

        if (item.getRequest() != null) {
            dto.setRequest(requestMapper.toSendDto(item.getRequest()));
        }
        dto.setOwner(userMapper.toSendDto(item.getOwner()));

        return dto;
    }

    // Упрощённая версия для случаев без дат и комментариев
    @Transactional(readOnly = true)
    private ItemSendDTO toSendDTO(Item item) {
        Object[] result = new Object[]{item};
        return toSendDTO(result, false, false);
    }

    private User findUserOrThrow(Long userId) {
        return userService.getByIdOrThrowInternal(userId);
    }

    private void validateDescriptionLength(String description) {
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Описание не может превышать " + MAX_DESCRIPTION_LENGTH + " символов");
        }
    }

    @Transactional
    private void updateItemFields(Item existingItem, ItemReqDTO dto) {
        boolean hasUpdates = false;

        if (dto.getName() != null) {
            existingItem.setName(dto.getName());
            hasUpdates = true;
        }
        if (dto.getDescription() != null) {
            validateDescriptionLength(dto.getDescription());
            existingItem.setDescription(dto.getDescription());
            hasUpdates = true;
        }
        if (dto.getAvailable() != null) {
            existingItem.setAvailable(dto.getAvailable());
            hasUpdates = true;
        }

        if (!hasUpdates) {
            throw new IllegalArgumentException("При обновлении хотя бы одно поле должно быть заполнено");
        }
    }

    @Transactional
    @Override
    public ItemSendDTO update(Long itemId, Long ownerId, ItemReqDTO dto) {
        Item item = getByIdOrThrowInternal(itemId);
        checkOwnership(item.getOwner().getId(), ownerId);
        updateItemFields(item, dto);

        return toSendDTO(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    @Override
    public ItemSendDTO getById(Long id) {
        List<Object[]> results = itemRepository.findItemWithBookingDatesAndCommentsById(id, validStatuses);
        if (results.isEmpty() || results.get(0)[0] == null) {
            throw new NoSuchElementException("Вещь с ID:" + id + " не существует");
        }
        return toSendDTO(results.get(0), false, true); // даты и комментарии
    }

    @Transactional(readOnly = true)
    @Override
    public Item getByIdOrThrowInternal(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Вещь с ID:" + id + " не существует"));
        return item;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemSendDTO> getOwnerItems(long ownerId) {
        List<Object[]> results = itemRepository.findItemsWithBookingDatesOnlyByOwnerId(ownerId, validStatuses);

        return results.stream()
                .map(arr -> toSendDTO(arr, true, false)) // даты есть, комментариев нет
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<ItemSendDTO> findItemsByRequestIdForRequester(Long requestId, Long requesterId) {
        ItemRequest request = requestService.findRequestByIdOrThrowInternal(requestId);
        checkOwnership(request.getRequester().getId(), requesterId);

        return itemRepository.findByRequestId(requestId).stream()
                .map(this::toSendDTO) // без дат и комментариев
                .toList();
    }

    @Transactional
    @Override
    public void deleteByIdForOwner(long ownerId, long itemId) {
        Item item = getByIdOrThrowInternal(itemId);
        checkOwnership(item.getOwner().getId(), ownerId);
        itemRepository.deleteById(itemId);
    }

    @Transactional(readOnly = true)
    private void checkOwnership(Long realOwner, Long possibleOwner) {
        if (!Objects.equals(realOwner, possibleOwner)) {
            throw new SecurityException(
                    String.format("Пользователь ID: %d пытался получить доступ к вещам пользователя ID: %d", possibleOwner, realOwner));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemSendDTO> search(String text) {
        String normalizedText = normalizeSearchText(text);
        if (normalizedText.isEmpty()) {
            return Collections.emptyList();
        }

        List<Item> items = itemRepository.searchItems(normalizedText);
        return items.stream()
                .map(this::toSendDTO) // без дат и комментариев
                .collect(Collectors.toList());
    }

    private String normalizeSearchText(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}

