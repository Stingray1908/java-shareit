package ru.practicum.shareit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.NoSuchElementException;

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

    @SneakyThrows
    public static RequestStatus fromId(Integer id) {
        for (RequestStatus rating : values()) {
            if (rating.getId() == id) {
                return rating;
            }
        }
        throw new NoSuchElementException("Неизвестный RequestStatus id: " + id);
    }
}

