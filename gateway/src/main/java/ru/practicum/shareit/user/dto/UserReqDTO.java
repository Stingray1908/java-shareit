package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.common.groups.OnCreate;
import ru.practicum.shareit.common.groups.OnUpdate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserReqDTO {
    @NotNull(groups = OnCreate.class, message = "Имя при создании не может быть null")
    @NotBlank(groups = OnCreate.class, message = "Имя при создании не может быть пусто")
    private String name;

    @NotNull(groups = OnCreate.class, message = "Email при создании не может быть null")
    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "Некорректный формат email")
    private String email;
}
