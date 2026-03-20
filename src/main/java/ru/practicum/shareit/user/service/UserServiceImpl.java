package ru.practicum.shareit.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    public UserServiceImpl(@Qualifier("InMemoryUserRepository")
                           UserRepository userRepository,
                           UserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Override
    public UserSendDTO create(UserReqDTO reqDTO) {
        User user = mapper.toEntity(reqDTO);
        String email = user.getEmail();

        if (userRepository.isEmailExist(email)) {
            throw new ConflictException("Пользователь с email: " + email + " уже существует");
        }

        return mapper.toSendDto(userRepository.save(user));
    }

    @Override
    public UserSendDTO update(Long id, UserReqDTO reqDTO) {
        User patchingUser = mapper.toEntity(reqDTO);
        patchingUser.setId(id);

        String name = patchingUser.getName();
        String email = patchingUser.getEmail();

        if (name == null && email == null)
            throw new IllegalArgumentException("Не заданы имя и email для обновления пользователя");

        Optional<User> opt = userRepository.findById(id);
        User existingUser = opt.orElseThrow(
                () -> new NoSuchElementException("Пользователь с ID " + id + " не найден"));

        if (name != null) {
            existingUser.setName(name);
        }

        if (email != null) {
            if (userRepository.isEmailExist(email)) {
                throw new ConflictException("Пользователь с email: " + email + " уже существует");
            }
            existingUser.setEmail(email);
        }

        return mapper.toSendDto(userRepository.update(existingUser));
    }

    @Override
    public UserSendDTO getById(Long id) {
        return mapper.toSendDto(getByIdInternal(id));
    }

    public User getByIdInternal(Long id) {
        Optional<User> opt = userRepository.findById(id);

        return opt.orElseThrow(() -> new NoSuchElementException("Пользователь с ID " + id + " не найден"));
    }

    @Override
    public Collection<UserSendDTO> getAll() {
        return userRepository.findAll().map(users -> users
                .stream()
                .map(mapper::toSendDto)
                .toList()).orElseGet(ArrayList::new);
    }

    @Override
    public void delete(Long id) {
        // вернунть оптионал, если пуст, вернуть NoSuchElementException и понятный текст
        Optional<User> opt = userRepository.deleteById(id);
        if (opt.isEmpty()) {
            throw new NoSuchElementException("Пользователь с ID " + id + " не найден");
        }
    }
}
