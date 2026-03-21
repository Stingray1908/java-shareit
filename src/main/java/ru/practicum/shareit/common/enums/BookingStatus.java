package ru.practicum.shareit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.NoSuchElementException;

@AllArgsConstructor
public enum BookingStatus {
    WAITING(1, "ожидает одобрения"),
    APPROVED(2, "подтверждено владельцем"),
    REJECTED(3, "отклонено владельцем"),
    CANCELED(4, "отменено создателем"),
    COMPLETED(5, "состоялось (вещь возвращена, бронирование завершено)");

    @Getter
    private final int id;
    @Getter
    private final String name;

    @SneakyThrows
    public static BookingStatus fromId(Integer id) {
        for (BookingStatus rating : values()) {
            if (rating.getId() == id) {
                return rating;
            }
        }
        throw new NoSuchElementException("Неизвестный BookingStatus id: " + id);
    }
}
