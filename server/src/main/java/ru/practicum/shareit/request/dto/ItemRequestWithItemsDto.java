package ru.practicum.shareit.request.dto;

import lombok.Data;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.item.dto.ItemShortDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemRequestWithItemsDto {
    private Long id;
    private String description;
    private UserSendDTO requester;
    private LocalDateTime created;
    private RequestStatus status;
    private List<ItemShortDto> items; // список вещей в ответе на запрос
}
