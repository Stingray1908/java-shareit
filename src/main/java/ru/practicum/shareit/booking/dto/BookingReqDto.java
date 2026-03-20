package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import ru.practicum.shareit.common.enums.BookingStatus;
import ru.practicum.shareit.common.groups.OnCreate;
import ru.practicum.shareit.common.groups.OnUpdate;

import java.time.LocalDateTime;

@Data
@Validated
@AllArgsConstructor
@NoArgsConstructor
public class BookingReqDto {
    @Null(groups = OnUpdate.class)
    private Long id;

    @Null(groups = OnUpdate.class)
    private Long bookerId;

    @Null(groups = OnUpdate.class)
    @NotNull(groups = OnCreate.class, message = "ID объекта бронирования не может быть null")
    @Positive(groups = OnCreate.class, message = "ID объекта бронирования должен быть положительным числом")
    private Long itemId;

    @Null(groups = OnUpdate.class)
    @NotNull(groups = OnCreate.class, message = "Дата и время начала бронирования не могут быть null")
    private LocalDateTime start;

    @Null(groups = OnUpdate.class)
    @NotNull(groups = OnCreate.class, message = "Дата и время окончания бронирования не могут быть null")
    private LocalDateTime end;

    @NotNull(groups = OnUpdate.class, message = "Статус не может быть null при обновлении")
    private BookingStatus status;
}
