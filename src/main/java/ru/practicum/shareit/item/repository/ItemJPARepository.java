package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.item.Item;

public interface ItemJPARepository extends JpaRepository<Item, Long> {



}
