package ru.practicum.shareit.item.dto;

import lombok.Data;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemSendDTO {
    private Long id;
    private UserSendDTO owner;
    private String name;
    private String description;
    private ItemRequestSendDTO request;
    private Boolean available;
    private LocalDateTime lastBooking;
    private LocalDateTime nextBooking;
    private List<CommentSendDto> comments;
}
