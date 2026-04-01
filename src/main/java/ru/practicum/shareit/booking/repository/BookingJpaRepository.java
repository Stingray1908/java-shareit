package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.Booking;

import java.time.LocalDateTime;
import java.util.List;


public interface BookingJpaRepository extends JpaRepository<Booking, Long> {

    @Query("""
    SELECT COUNT(b) > 0
    FROM Booking b
    WHERE b.item.id = :itemId
      AND b.status IN (APPROVED, WAITING)
      AND b.start < :end
      AND b.end > :start
""")
    boolean checkOverLapBookings(
            @Param("itemId") Long itemId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
    SELECT b FROM Booking b
    WHERE b.id = :id
      AND b.status IN (APPROVED, WAITING)
""")
Booking findByIdWithActiveStatus(
    @Param("id") Long id
);


    Booking save(Booking booking);
}
