package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemReqDTO {
    @NotNull(message = "Название не может быть пустым")
    @NotBlank(message = "Название не может содержать только пробелы")
    private String name;

    @NotNull(message = "Описание не может быть пустым")
    @NotBlank(message = "Описание не может содержать только пробелы")
    private String description;

    @Positive(message = "ID запроса должен быть положительным числом")
    private Long requestId;

    @NotNull(message = "Статус доступности не может быть пустым")
    private Boolean available;
}

