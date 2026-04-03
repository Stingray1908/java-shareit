package ru.practicum.shareit.item.service;


import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingDates;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.request.service.RequestService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;

@Slf4j

@RequiredArgsConstructor
@Service("ItemJPAService")
public class ItemJPAService implements ItemService{

    private ItemJPARepository itemRepository;
    private UserService userService;
    private RequestService requestService;
    private ItemMapper itemMapper;
    private RequestMapper requestMapper;
    private UserMapper userMapper;

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    public ItemJPAService(
            ItemJPARepository itemRepository,
            ItemMapper itemMapper,
            UserService userService,
            UserMapper userMapper,
            RequestService requestService,
            RequestMapper requestMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.userService = userService;
        this.userMapper = userMapper;
        this.requestService = requestService;
        this.requestMapper = requestMapper;
    }

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

    private ItemSendDTO toSendDTO(Item item) {
        ItemSendDTO dto = itemMapper.toSendDto(item);;
        if (item.getRequest() != null) {
            dto.setRequest(requestMapper.toSendDto(item.getRequest()));
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

    private void validateDescription(String description) {
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("DESCRIPTION_LENGTH_ERROR");
        }
    }

    //тест
    @Override
    public ItemSendDTO update(Long itemId, Long ownerId, ItemReqDTO dto) {
        Item item = getByIdOrThrowInternal(itemId);
        checkOwnership(item.getOwner().getId(), ownerId);
        updateItemFields(item, dto);

        return toSendDTO(itemRepository.save(item));
    }

    //тест
    @Override
    public ItemSendDTO getById(Long id) {
        return toSendDTO(getByIdOrThrowInternal(id));
    }

    @Override
    public Item getByIdOrThrowInternal(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(()-> new NoSuchElementException("Вещь с ID:"+id+ " не существует"));
    }

    //test
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
                        dto.setLastBookingDate(times.lastBooking());
                        dto.setNextBookingDate(times.nextBooking());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /*public List<Map<String, Object>> find(Long id){
        return itemRepository.findBookingsByItemId(id);
    }*/





    //тест
    @Override
    public Collection<ItemSendDTO> findItemsByRequestIdForRequester(Long requestId, Long requesterId) {
        ItemRequest request = requestService.findRequestByIdOrThrowInternal(requestId);
        checkOwnership(request.getRequester().getId(), requesterId);

        return itemRepository.findByRequestId(requestId).stream()
                .map(this::toSendDTO)
                .toList();
    }

    //тест
    @Override
    public void deleteByIdForOwner(long ownerId, long itemId) {
        Item item = getByIdOrThrowInternal(itemId);
        checkOwnership(item.getOwner().getId(), ownerId);
        itemRepository.deleteById(itemId);
    }

    private void checkOwnership(Long realOwner, Long possibleOwner) {
        if (! Objects.equals(realOwner, possibleOwner)) {
        throw new SecurityException(
                String.format("Пользователь ID: %d пытался получить доступ к вещам пользователя ID: %d", possibleOwner, realOwner));
    }
    }

    //тест
    @Override
    public List<ItemSendDTO> search(String text) {
        text = text == null ? "" : text.trim().toLowerCase();
        return itemRepository.searchItems(text).stream()
                .map(this::toSendDTO)
                .toList();
    }
}

