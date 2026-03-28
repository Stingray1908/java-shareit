package ru.practicum.shareit.user.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;

import java.util.Collection;
import java.util.List;

@Service("JpaService")
public class UserJPAService implements UserService {

    private final UserJPARepository jpaRepository;
    private final UserMapper mapper;

    public UserJPAService(
            UserJPARepository jpaRepository,
            UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UserSendDTO create(UserReqDTO reqDTO) {
        User user = mapper.toEntity(reqDTO);
        String email = user.getEmail();

        if (jpaRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email: " + email + " уже существует");
        }

        return mapper.toSendDto(jpaRepository.save(user));
    }

    @Override
    public UserSendDTO update(Long id, UserReqDTO reqDTO) {
        return null;
    }

    @Override
    public UserSendDTO getById(Long id) {
        return null;
    }

    @Override
    public User getByIdInternal(Long id) {
        return null;
    }

    @Override
    public Collection<UserSendDTO> getAll() {
        return List.of();
    }

    @Override
    public void delete(Long id) {

    }
}

