package ru.practicum.shareit.user.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.User;

import java.util.*;

@Repository("InMemoryUserRepository")
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new HashMap<>();
    private Long nextId = 1L;

    @Override
    public User save(User user) {
        Long id = nextId++;
        user.setId(id);

        users.put(id, user);
        return user;
    }

    @Override
    public User update(User user) {

        return users.put(user.getId(), user);
    }

    @Override
    public Optional<User> deleteById(Long id) {

        return Optional.ofNullable(users.remove(id));
    }

    @Override
    public Optional<User> findById(Long id) {

        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<Collection<User>> findAll() {

        return Optional.of(users.values());
    }

    @Override
    public boolean isEmailExist(String email) {
        return users.values().stream()
                .anyMatch(u -> Objects.equals(u.getEmail(), email));
    }

    // для тестов
    public void clear() {
        users.clear();
        nextId = 1L;
    }
}
