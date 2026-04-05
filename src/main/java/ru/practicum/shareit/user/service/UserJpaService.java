package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserJpaRepository;

import java.util.Collection;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class UserJpaService implements UserService {

    private final UserJpaRepository jpaRepository;
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
        User existingUser = getByIdOrThrowInternal(id);
        updateUserFields(existingUser, reqDTO);
        return mapper.toSendDto(jpaRepository.save(existingUser));
    }

    private void updateUserFields(User existingUser, UserReqDTO reqDTO) {
        String newName = reqDTO.getName();
        String newEmail = reqDTO.getEmail();

        if (newName == null && newEmail == null) {
            throw new IllegalArgumentException("Не заданы имя и email для обновления пользователя");
        }

        if (newName != null) {
            existingUser.setName(newName);
        }

        if (newEmail != null) {
            isNotEmailExistOrThrow(newEmail);
            existingUser.setEmail(newEmail);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public UserSendDTO getById(Long id) {
        return mapper.toSendDto(getByIdOrThrowInternal(id));
    }

    @Transactional(readOnly = true)
    public User getByIdOrThrowInternal(Long id) {
        return jpaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с id: " + id + " не существует"));
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
