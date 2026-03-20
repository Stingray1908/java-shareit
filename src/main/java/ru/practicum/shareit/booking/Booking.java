package ru.practicum.shareit.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.common.enums.BookingStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Booking {
    @EqualsAndHashCode.Include
    private Long id;
    @EqualsAndHashCode.Include
    private Long bookerId;
    @EqualsAndHashCode.Include
    private Long itemId;
    private LocalDateTime start;
    private LocalDateTime end;
    private BookingStatus status;

}
