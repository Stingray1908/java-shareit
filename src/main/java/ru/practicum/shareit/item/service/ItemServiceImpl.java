package ru.practicum.shareit.item.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.service.RequestService;
import ru.practicum.shareit.user.service.UserService;

import java.util.*;
import java.util.stream.Collectors;

@Service("itemServiceMemory")
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final RequestService requestService;
    private final ItemMapper itemMapper;

    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private static final String DESCRIPTION_LENGTH_ERROR =
            "Описание не может превышать " + MAX_DESCRIPTION_LENGTH + " символов";

    public ItemServiceImpl(
            @Qualifier("InMemoryItemRepository") ItemRepository itemRepository,
            ItemMapper itemMapper,
            UserService userService,
            RequestService requestService) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.userService = userService;
        this.requestService = requestService;
    }

    @Override
    public ItemSendDTO create(Long ownerId, @Valid ItemReqDTO itemReqDTO) {
        validateUserExists(ownerId);

        Item item = itemMapper.toEntity(itemReqDTO);
        validateDescription(item.getDescription());

        Long requestId = item.getRequestId();
        if (requestId != null) {
            updateRequestStatus(requestId);
        }

        item.setOwnerId(ownerId);
        Item savedItem = itemRepository.create(item);
        log.debug("Создана вещь с ID: {} и requestId: {}", savedItem.getId(), requestId);

        return itemMapper.toSendDto(savedItem);
    }

    private void updateRequestStatus(Long requestId) {
        ItemRequest request = requestService.getByIdForInternal(requestId);

        if (RequestStatus.PENDING.equals(request.getStatus())) {
            request.setStatus(RequestStatus.RESPONDED);
            requestService.patchStatusInternal(request);
        }
    }

    @Override
    public ItemSendDTO update(Long itemId, Long ownerId, ItemReqDTO itemReqDTO) {
        Item existingItem = getByIdInternal(itemId);
        updateItemFields(existingItem, itemReqDTO);

        if (existingItem.getDescription() != null) {
            validateDescription(existingItem.getDescription());
        }

        return itemMapper.toSendDto(itemRepository.update(itemId, ownerId, existingItem));
    }

    private void updateItemFields(Item existingItem, ItemReqDTO dto) {
        boolean hasUpdates = false;

        if (dto.getName() != null) {
            existingItem.setName(dto.getName());
            hasUpdates = true;
        }
        if (dto.getDescription() != null) {
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

    public Item getByIdInternal(Long id) {
        return itemRepository.getById(id)
                .orElseThrow(() -> new NoSuchElementException("Вещь с ID: " + id + " не найдена"));
    }

    @Override
    public ItemSendDTO getById(Long id) {
        return itemMapper.toSendDto(getByIdInternal(id));
    }

    @Override
    public List<ItemSendDTO> getOwnerItems(long ownerId) {
        validateUserExists(ownerId);
        return itemRepository.findByUserId(ownerId).stream()
                .map(itemMapper::toSendDto)
                .toList();
    }

    @Override
    public Collection<ItemSendDTO> getItemsByRequestOwnerAndRequestIds(Long requestId, Long requestOwnerId) {
        validateUserExists(requestOwnerId);
        validateRequestOwnership(requestOwnerId, requestId);

        return itemRepository.getItemsByRequestIds(requestId).stream()
                .map(itemMapper::toSendDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByItemAndOwnerIds(long ownerId, long itemId) {
        validateUserExists(ownerId);
        log.info("Попытка удаления вещи ID: {} владельцем ID: {}", itemId, ownerId);

        boolean wasDeleted = itemRepository.deleteByItemAndOwnerIds(itemId, ownerId);
        if (!wasDeleted) {
            throw new SecurityException(
                    String.format("Пользователь ID: %d не является владельцем вещи ID: %d", ownerId, itemId));
        }
        log.info("Вещь ID: {} успешно удалена владельцем ID: {}", itemId, ownerId);
    }


    @Override
    public List<ItemSendDTO> search(String text) {
        String normalizedText = normalizeSearchText(text);
        if (normalizedText.isEmpty()) {
            return Collections.emptyList();
        }

        return itemRepository.search(normalizedText).stream()
                .map(itemMapper::toSendDto)
                .toList();
    }

    private String normalizeSearchText(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private void validateDescription(String description) {
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(DESCRIPTION_LENGTH_ERROR);
        }
    }

    private void validateUserExists(Long userId) {
        userService.getByIdInternal(userId);
    }

    private void validateRequestOwnership(Long requestOwnerId, Long requestId) {
        ItemRequest request = requestService.getByIdForInternal(requestId);

        if (!Objects.equals(request.getRequester(), requestOwnerId)) {
            throw new SecurityException(
                    String.format("Пользователь ID: %d не является владельцем запроса ID: %d",
                            requestOwnerId, requestId));
        }
    }
}
