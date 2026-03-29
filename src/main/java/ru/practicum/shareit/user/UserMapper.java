package ru.practicum.shareit.user;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.common.GenericMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;

@Component
public class UserMapper implements GenericMapper<User, UserReqDTO, UserSendDTO> {

    @Override
    public UserSendDTO toSendDto(User user) {
        if (user == null) return null;

        return new UserSendDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    @Override
    public UserReqDTO toReqDto(User user) {
        if (user == null) return null;

        return new UserReqDTO(
                user.getName(),
                user.getEmail()
        );
    }

    @Override
    public User toEntity(UserReqDTO dto) {
        if (dto == null) return null;

        return new User(
                dto.getName(),
                dto.getEmail()
        );
    }

    public User toEntity(UserSendDTO dto) {
        if (dto == null) return null;

        User user = new User();
        user.setId(dto.getId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return user;
    }
}
