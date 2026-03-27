package ru.practicum.shareit.request;

import lombok.Data;
import ru.practicum.shareit.common.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemRequest {
    private Long id;
    private String description;
    private Long requester;
    private LocalDateTime created;
    private RequestStatus status;
    private List<Long> itemIds = new ArrayList<>(10);
}
