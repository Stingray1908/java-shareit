package ru.practicum.shareit.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.user.User;

public interface UserJPARepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    void deleteAll();
}
