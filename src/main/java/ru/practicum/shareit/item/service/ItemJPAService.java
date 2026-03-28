package ru.practicum.shareit.item.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemReqDTO;
import ru.practicum.shareit.item.dto.ItemSendDTO;
import ru.practicum.shareit.item.repository.ItemJPARepository;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service("ItemJPAService")
public class ItemJPAService implements ItemService{

    private final ItemMapper itemMapper = new ItemMapper();
    private final ItemJPARepository itemRepository;

    @Override
    public ItemSendDTO create(Long ownerId, ItemReqDTO itemReqDTO) {
        return null;
    }

    @Override
    public ItemSendDTO update(Long itemId, Long ownerId, ItemReqDTO itemReqDTO) {
        return null;
    }

    @Override
    public ItemSendDTO getById(Long id) {
        return null;
    }

    @Override
    public Item getByIdInternal(Long id) {
        return null;
    }

    @Override
    public List<ItemSendDTO> getOwnerItems(long userId) {
        return List.of();
    }

    @Override
    public Collection<ItemSendDTO> getItemsByRequestOwnerAndRequestIds(Long requestId, Long requestOwnerId) {
        return List.of();
    }

    @Override
    public void deleteByItemAndOwnerIds(long userId, long itemId) {

    }

    @Override
    public List<ItemSendDTO> search(String text) {
        return List.of();
    }
}

