package ru.practicum.shareit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.NoSuchElementException;

@AllArgsConstructor
public enum ItemStatus {
    AVAILABLE(1, "доступно"),
    UNAVAILABLE(2, "недоступно");

    @Getter
    private final int id;
    @Getter
    private final String name;

    @SneakyThrows
    public static ItemStatus fromId(Integer id) {
        for (ItemStatus rating : values()) {
            if (rating.getId() == id) {
                return rating;
            }
        }
        throw new NoSuchElementException("Неизвестный ItemStatus id: " + id);
    }
}
