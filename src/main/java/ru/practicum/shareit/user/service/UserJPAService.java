package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJPARepository;

import java.util.Collection;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class UserJPAService implements UserService {

    private final UserJPARepository jpaRepository;
    private final UserMapper mapper;

    @Transactional
    @Override
    public UserSendDTO create(UserReqDTO reqDTO) {
        User user = mapper.toEntity(reqDTO);

        isNotEmailExistOrThrow(user.getEmail());

        return mapper.toSendDto(jpaRepository.save(user));
    }

    @Transactional
    @Override
    public UserSendDTO update(Long id, UserReqDTO reqDTO) {
        User patchingUser = mapper.toEntity(reqDTO);
        patchingUser.setId(id);

        String name = patchingUser.getName();
        String email = patchingUser.getEmail();

        if (name == null && email == null)
            throw new IllegalArgumentException("Не заданы имя и email для обновления пользователя");

        User existingUser = getByIdOrThrowInternal(id);

        if (name != null) {
            existingUser.setName(name);
        }

        if (email != null) {
            isNotEmailExistOrThrow(email);
            existingUser.setEmail(email);
        }

        return mapper.toSendDto(jpaRepository.save(existingUser));
    }

    @Transactional(readOnly = true)
    @Override
    public UserSendDTO getById(Long id) {
        return mapper.toSendDto(
                getByIdOrThrowInternal(id)
        );
    }

    @Transactional(readOnly = true)
    public User getByIdOrThrowInternal(Long id) {
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        return jpaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с id: " + id + " не существует"));
    }

    @Transactional(readOnly = true)
    public UserSendDTO getUserWithItems(Long id) {
        User user = jpaRepository.findUserWithItems(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с id: " + id + " не существует"));

        return mapper.toSendDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<UserSendDTO> getAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toSendDto)
                .toList();
    }

    @Transactional
    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    private void isNotEmailExistOrThrow(String email) {
        if (jpaRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email: " + email + " уже существует");
        }
    }
}
