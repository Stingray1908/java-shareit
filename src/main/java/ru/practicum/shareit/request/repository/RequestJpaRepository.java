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

    @Query("SELECT r FROM ItemRequest r WHERE r.id = :id AND r.status IN ('PENDING', 'RESPONDED')")
    Optional<ItemRequest> findByIdWithActiveStatus(@Param("id") Long id);

    @NonNull
    Optional<ItemRequest> findById(@NonNull Long id);

    List<ItemRequest> findAllByRequesterId(Long id);

    void deleteById(Long id);
}
