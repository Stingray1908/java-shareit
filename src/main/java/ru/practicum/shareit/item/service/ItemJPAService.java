package ru.practicum.shareit.item.service;


import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.request.service.RequestService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j

@RequiredArgsConstructor
@Service("ItemJPAService")
public class ItemJPAService implements ItemService{

    private ItemJPARepository itemRepository;
    private UserService userService;
    private RequestService requestService;
    private ItemMapper itemMapper;

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    public ItemJPAService(
            ItemJPARepository itemRepository,
            ItemMapper itemMapper,
            UserService userService,
            RequestService requestService) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.userService = userService;
        this.requestService = requestService;
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

        Item saved = itemRepository.save(item);
        log.debug("Создана вещь с ID: {} для запроса с ID: {}", saved.getId(), requestId);
        return itemMapper.toSendDto(saved);
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

        return itemMapper.toSendDto(itemRepository.save(item));
    }

    //тест
    @Override
    public ItemSendDTO getById(Long id) {
        return itemMapper.toSendDto(getByIdOrThrowInternal(id));
    }

    @Override
    public Item getByIdOrThrowInternal(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(()-> new NoSuchElementException("Вещь с ID:"+id+ " не существует"));
    }

    //test
    @Override
    public List<ItemSendDTO> getOwnerItems(long userId) {
        return itemRepository.findByOwnerId(userId).stream()
                .map(itemMapper::toSendDto)
                .toList();
    }

    //тест
    @Override
    public Collection<ItemSendDTO> findItemsByRequestIdForRequester(Long requestId, Long requesterId) {
        ItemRequest request = requestService.findRequestByIdOrThrowInternal(requestId);
        checkOwnership(request.getRequester().getId(), requesterId);

        return itemRepository.findByRequestId(requestId).stream()
                .map(itemMapper::toSendDto)
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
                .map(itemMapper::toSendDto)
                .toList();
    }
}

