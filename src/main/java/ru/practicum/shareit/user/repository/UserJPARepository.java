package ru.practicum.shareit.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import ru.practicum.shareit.user.User;

import java.util.List;
import java.util.Optional;

public interface UserJPARepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    @NonNull
    Optional<User> findById(@NonNull Long id);

    @NonNull
    List<User> findAll();

    void deleteById(@NonNull Long id);

    void deleteAll();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.items WHERE u.id = :id")
    Optional<User> findUserWithItems(@Param("id") Long id);
}
