package ru.practicum.shareit.request.dto;

import lombok.Data;
import ru.practicum.shareit.common.enums.RequestStatus;

import java.time.LocalDateTime;

@Data
public class ItemRequestSendDTO {
    private Long id;
    private String description;
    private Long requestor;
    private LocalDateTime created;
    private RequestStatus status;
}
