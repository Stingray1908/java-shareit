package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.Item;

import java.util.List;
import java.util.Map;

public interface ItemJPARepository extends JpaRepository<Item, Long> {

    List<Item> findByRequestId(Long id);

    @Query("SELECT i FROM Item i WHERE " +
            "LOWER(i.name) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(i.description) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<Item> searchItems(@Param("text") String text);


    @Query("SELECT i FROM Item i WHERE i.owner.id = :ownerId ORDER BY i.id")
    List<Item> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query(value = "SELECT " +
            "  i.id AS item_id, " +
            "  MAX(CASE WHEN b.\"end\" < NOW() THEN b.\"end\" END) AS last_booking_end, " +
            "  MIN(CASE WHEN b.start > NOW() THEN b.start END) AS next_booking_start " +
            "FROM items i " +
            "LEFT JOIN bookings b ON i.id = b.item_id " +
            "  AND b.status IN ('APPROVED', 'WAITING', 'COMPLETED') " +
            "WHERE i.owner_id = :ownerId " +
            "GROUP BY i.id",
            nativeQuery = true)
    List<Map<String, Object>> findItemBookingTimesByOwnerId(@Param("ownerId") Long ownerId);
}

