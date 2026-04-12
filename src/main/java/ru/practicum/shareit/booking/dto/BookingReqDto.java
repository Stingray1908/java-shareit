package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.common.groups.OnCreate;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingReqDto {
    @NotNull(groups = OnCreate.class, message = "ID объекта бронирования не может быть null")
    @Positive(groups = OnCreate.class, message = "ID объекта бронирования должен быть положительным числом")
    private Long itemId;

    @NotNull(groups = OnCreate.class, message = "Дата и время начала бронирования не могут быть null")
    private LocalDateTime start;

    @NotNull(groups = OnCreate.class, message = "Дата и время окончания бронирования не могут быть null")
    private LocalDateTime end;
}
