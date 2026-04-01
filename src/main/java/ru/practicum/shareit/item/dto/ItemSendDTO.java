package ru.practicum.shareit.item.dto;

import lombok.Data;
import lombok.Value;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserSendDTO;

@Data
public class ItemSendDTO {
    Long id;
    UserSendDTO owner;
    String name;
    String description;
    ItemRequestSendDTO request;
    Boolean available;
}

