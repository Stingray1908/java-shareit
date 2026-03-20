package ru.practicum.shareit.item;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.common.GenericMapper;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;

@Component
public class ItemMapper implements GenericMapper<Item, ItemReqDTO, ItemSendDTO> {

    @Override
    public ItemSendDTO toSendDto(Item item) {
        if (item == null) {
            return null;
        }

        return new ItemSendDTO(
                item.getId(),
                item.getOwnerId(),
                item.getName(),
                item.getDescription(),
                item.getRequestId(),
                item.getAvailable());
    }

    @Override
    public ItemReqDTO toReqDto(Item item) {
        if (item == null) {
            return null;
        }

        ItemReqDTO dto = new ItemReqDTO();
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setRequestId(item.getRequestId());
        dto.setAvailable(item.getAvailable());
        return dto;
    }

    @Override
    public Item toEntity(ItemReqDTO dto) {
        if (dto == null) {
            return null;
        }

        Item item = new Item();

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setRequestId(dto.getRequestId());
        item.setAvailable(dto.getAvailable());
        return item;
    }
}
