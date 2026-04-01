package ru.practicum.shareit.request;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.common.GenericMapper;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;

@Component
public class RequestMapper implements GenericMapper<ItemRequest, ItemRequestReqDTO, ItemRequestSendDTO> {

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
}
