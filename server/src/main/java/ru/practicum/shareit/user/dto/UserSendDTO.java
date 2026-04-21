package ru.practicum.shareit.user.dto;

import lombok.Value;

@Value
public class UserSendDTO {
    Long id;
    String name;
    String email;
}
