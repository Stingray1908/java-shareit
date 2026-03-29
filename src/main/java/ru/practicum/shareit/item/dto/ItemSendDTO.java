package ru.practicum.shareit.item.dto;

import lombok.Value;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

@Value
public class ItemSendDTO {
    Long id;
    User owner;
    String name;
    String description;
    ItemRequest request;
    Boolean available;
}

