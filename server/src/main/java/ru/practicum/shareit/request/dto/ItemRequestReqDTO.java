package ru.practicum.shareit.request.dto;

import lombok.Data;
import ru.practicum.shareit.common.enums.RequestStatus;

@Data
public class ItemRequestReqDTO {
    private String description;
    private RequestStatus status;
}

