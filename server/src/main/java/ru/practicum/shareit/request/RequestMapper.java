package ru.practicum.shareit.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.common.GenericMapper;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.user.UserMapper;

@Component
public class RequestMapper implements GenericMapper<ItemRequest, ItemRequestReqDTO, ItemRequestSendDTO> {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ItemRequestSendDTO toSendDto(ItemRequest entity) {
        if (entity == null) {
            return null;
        }

        ItemRequestSendDTO dto = new ItemRequestSendDTO();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setCreated(entity.getCreated());
        dto.setStatus(entity.getStatus());
        // Реквестера добовлять отдельно
        return dto;
    }

    @Override
    public ItemRequestReqDTO toReqDto(ItemRequest entity) {
        if (entity == null) {
            return null;
        }

        ItemRequestReqDTO dto = new ItemRequestReqDTO();
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    @Override
    public ItemRequest toEntity(ItemRequestReqDTO dto) {
        if (dto == null) {
            return null;
        }

        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());

        // Устанавливаем статус, если он передан, иначе оставляем по умолчанию (PENDING)
        request.setStatus(dto.getStatus() != null ? dto.getStatus() : RequestStatus.PENDING);
        return request;
    }

    public ItemRequestWithItemsDto toWithItemsDto(ItemRequest entity) {
        if (entity == null) {
            return null;
        }

        ItemRequestWithItemsDto dto = new ItemRequestWithItemsDto();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setCreated(entity.getCreated());
        dto.setStatus(entity.getStatus());
        dto.setRequester(userMapper.toSendDto(entity.getRequester()));
        dto.setItems(entity.getItems().stream()
                .map(this::toItemShortDto)
                .toList());
        return dto;
    }

    private ItemShortDto toItemShortDto(Item item) {
        ItemShortDto dto = new ItemShortDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setOwnerId(item.getOwner().getId());
        return dto;
    }
}
