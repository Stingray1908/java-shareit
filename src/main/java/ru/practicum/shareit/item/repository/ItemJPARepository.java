package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.Item;

import java.util.List;

public interface ItemJPARepository extends JpaRepository<Item, Long> {

    List<Item> findByOwnerId(Long id);

    List<Item> findByRequestId(Long id);

    @Query("SELECT i FROM Item i WHERE " +
            "LOWER(i.name) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(i.description) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<Item> searchItems(@Param("text") String text);
}

