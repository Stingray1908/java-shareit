package ru.practicum.shareit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum RequestStatus {
    PENDING(1, "в ожидаении"),
    RESPONDED(2, "есть отклик"),
    CANCELLED(3, "отменён"),
    COMPLETED(4, "выполнен");

    @Getter
    private final int id;
    @Getter
    private final String name;
}

