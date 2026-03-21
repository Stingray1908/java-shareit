package ru.practicum.shareit.item.dto;

import lombok.Value;

@Value
public class ItemSendDTO {
    Long id;
    Long ownerId;
    String name;
    String description;
    Long requestId;
    Boolean available;
}

