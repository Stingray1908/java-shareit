package ru.practicum.shareit.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.common.groups.OnUpdate;

@Data
public class ItemRequestReqDTO {
    @NotNull(message = "Описание запроса не может быть пустым")
    @NotBlank(message = "Описание запроса не может содержать только пробелы")
    @Size(min = 10, max = 200, message = "длина описания должна быть от 10 до 200 символов")
    private String description;
    @NotNull(groups = OnUpdate.class)
    private RequestStatus status;
}

