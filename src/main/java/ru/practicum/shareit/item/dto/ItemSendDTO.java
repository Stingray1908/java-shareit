package ru.practicum.shareit.item.dto;

import lombok.Data;
import lombok.Value;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserSendDTO;

import java.time.LocalDateTime;

@Data
public class ItemSendDTO {
    private Long id;
    private UserSendDTO owner;
    private String name;
    private String description;
    private ItemRequestSendDTO request;
    private Boolean available;
    private LocalDateTime lastBookingDate;
    private LocalDateTime nextBookingDate;
}

