package ru.practicum.shareit.item.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingDates;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.item.comment.CommentMapper;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentRepository;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserJPAService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j

@RequiredArgsConstructor
@Service("ItemJPAService")
public class ItemJPAService implements ItemService{

    private final ItemJPARepository itemRepository;
    private final UserJPAService userService;
    private final RequestJpaService requestService;

    private final ItemMapper itemMapper;
    private final RequestMapper requestMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    private final CommentRepository commentRepository;
    private final BookingJpaRepository bookingRepository;

    private static final int MAX_DESCRIPTION_LENGTH = 100;

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
        System.out.println("текст" + dto);
        // Проверка: пользователь действительно брал вещь в аренду
        if (!hasUserBookedItem(userId, itemId)) {
            throw new IllegalArgumentException("Пользователь не брал эту вещь в аренду, поэтому не может оставить отзыв");
        }
        System.out.println("текст" + dto);
        // Создаём новый комментарий
        Comment comment = new Comment();
        comment.setBooker(booker);
        comment.setItem(item);
        comment.setText(dto.getText());
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        // Сохраняем в БД
        Comment savedComment = commentRepository.save(comment);

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
        List<String> validStatuses = List.of("APPROVED", "COMPLETED");

        return bookingRepository.existsPastBooking(
                userId,
                itemId,
                validStatuses,
                now
        );
    }

    @Transactional(readOnly = true)
    private ItemSendDTO toSendDTO(Item item) {
        ItemSendDTO dto = itemMapper.toSendDto(item);;
        if (item.getRequest() != null) {
            dto.setRequest(requestMapper.toSendDto(item.getRequest()));
        }

        if (item.getComments() != null && !item.getComments().isEmpty()) {
            dto.setComments(item.getComments().stream()
                    .map(c -> commentMapper.toSendDto(c))
                    .collect(Collectors.toList()));
        }
        dto.setOwner(userMapper.toSendDto(item.getOwner()));
        return dto;
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

    @Transactional(readOnly = true)
    private void validateDescription(String description) {
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("DESCRIPTION_LENGTH_ERROR");
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
        return toSendDTO(getByIdOrThrowInternal(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Item getByIdOrThrowInternal(Long id) {
        System.out.println("++++++++++++++++++++++++++item+++++++++++++++++++++++++++++++");
        Item item = itemRepository.findById(id)
                .orElseThrow(()-> new NoSuchElementException("Вещь с ID:"+id+ " не существует"));
        System.out.println("=-=-=-=-===-=-=-=-=-==-=-=");
        return item;
    }

    @Transactional(readOnly = true)
    public List<ItemSendDTO> getOwnerItems(long ownerId) {
        LocalDateTime now = LocalDateTime.now();

        // Шаг 1: Получаем все вещи владельца
        List<Item> items = itemRepository.findByOwnerId(ownerId);

        // Если вещей нет, возвращаем пустой список
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        // Извлекаем ID всех вещей для использования в следующем запросе
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();

        // Шаг 2: Получаем данные о бронированиях только для этих вещей
        List<Map<String, Object>> bookingData = itemRepository.findItemBookingTimesByOwnerId(ownerId);

        // Создаём карту: itemId → (last_booking_end, next_booking_start)
        Map<Long, BookingDates> bookingTimesByItem = bookingData.stream()
                .collect(Collectors.toMap(
                        map -> (Long) map.get("item_id"),
                        map -> new BookingDates(
                                Optional.ofNullable(map.get("last_booking_end"))
                                        .filter(Timestamp.class::isInstance)
                                        .map(Timestamp.class::cast)
                                        .map(Timestamp::toLocalDateTime)
                                        .orElse(null),
                                Optional.ofNullable(map.get("next_booking_start"))
                                        .filter(Timestamp.class::isInstance)
                                        .map(Timestamp.class::cast)
                                        .map(Timestamp::toLocalDateTime)
                                        .orElse(null)
                        )));

        // Шаг 3: Обрабатываем каждый предмет
        return items.stream()
                .map(item -> {
                    ItemSendDTO dto = toSendDTO(item);
                    BookingDates times = bookingTimesByItem.get(item.getId());

                    if (times != null) {
                        dto.setLastBooking(times.lastBooking());
                        dto.setNextBooking(times.nextBooking());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /*public List<Map<String, Object>> find(Long id){
        return itemRepository.findBookingsByItemId(id);
    }*/





    @Transactional(readOnly = true)
    @Override
    public Collection<ItemSendDTO> findItemsByRequestIdForRequester(Long requestId, Long requesterId) {
        ItemRequest request = requestService.findRequestByIdOrThrowInternal(requestId);
        checkOwnership(request.getRequester().getId(), requesterId);

        return itemRepository.findByRequestId(requestId).stream()
                .map(this::toSendDTO)
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
        if (! Objects.equals(realOwner, possibleOwner)) {
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

        return itemRepository.searchItems(normalizedText).stream()
                .map(this::toSendDTO)
                .toList();
    }

    private String normalizeSearchText(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}

