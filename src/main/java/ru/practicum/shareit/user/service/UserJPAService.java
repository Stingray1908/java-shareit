package ru.practicum.shareit.user.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;

import java.util.Collection;
import java.util.NoSuchElementException;

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

        isNotEmailExistOrThrow(user.getEmail());

        return mapper.toSendDto(jpaRepository.save(user));
    }

    @Override
    public UserSendDTO update(Long id, UserReqDTO reqDTO) {
        User patchingUser = mapper.toEntity(reqDTO);
        patchingUser.setId(id);

        String name = patchingUser.getName();
        String email = patchingUser.getEmail();

        if (name == null && email == null)
            throw new IllegalArgumentException("Не заданы имя и email для обновления пользователя");

        User existingUser = findUserByIdInternalOrThrow(id);

        if (name != null) {
            existingUser.setName(name);
        }

        if (email != null) {
            isNotEmailExistOrThrow(email);
            existingUser.setEmail(email);
        }

        return mapper.toSendDto(jpaRepository.save(existingUser));
    }

    @Override
    public UserSendDTO getById(Long id) {
        return mapper.toSendDto(
                findUserByIdInternalOrThrow(id)
        );
    }

    @Override
    public User getByIdInternal(Long id) {
        return findUserByIdInternalOrThrow(id);
    }

    @Override
    public Collection<UserSendDTO> getAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toSendDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    private User findUserByIdInternalOrThrow(Long id) {
        return jpaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с id: " + id + " не существует"));
    }

    private void isNotEmailExistOrThrow(String email) {
        if (jpaRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email: " + email + " уже существует");
        }
    }


}

