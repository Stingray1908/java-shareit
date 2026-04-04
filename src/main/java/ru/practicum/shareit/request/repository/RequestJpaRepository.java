package ru.practicum.shareit.request.repository;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNullApi;
import ru.practicum.shareit.request.ItemRequest;

import java.util.List;
import java.util.Optional;

public interface RequestJpaRepository extends JpaRepository<ItemRequest, Long> {

    @NonNull
    ItemRequest save(@NonNull ItemRequest request);

    // Исправлен: добавлен JOIN FETCH для загрузки User
    @Query("SELECT r FROM ItemRequest r JOIN FETCH r.requester WHERE r.id = :id AND r.status IN ('PENDING', 'RESPONDED')")
    Optional<ItemRequest> findByIdWithActiveStatus(@Param("id") Long id);

    // Исправлен: добавлен JOIN FETCH для загрузки User
    @Query("SELECT r FROM ItemRequest r JOIN FETCH r.requester WHERE r.id = :id")
    Optional<ItemRequest> findById(@NonNull Long id);

    // Исправлен: добавлен JOIN FETCH для загрузки User
    @Query("SELECT r FROM ItemRequest r JOIN FETCH r.requester WHERE r.requester.id = :requesterId")
    List<ItemRequest> findAllByRequesterId(@Param("requesterId") Long requesterId);

    void deleteById(Long id);
}
