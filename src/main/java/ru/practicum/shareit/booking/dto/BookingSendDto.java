package ru.practicum.shareit.booking.dto;

import lombok.Value;
import ru.practicum.shareit.common.enums.BookingStatus;

import java.time.LocalDateTime;

@Value
public class BookingSendDto {
    Long id;
    Long bookerId;
    Long itemId;
    LocalDateTime start;
    LocalDateTime end;
    BookingStatus status;
}
