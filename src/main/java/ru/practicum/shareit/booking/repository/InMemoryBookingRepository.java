package ru.practicum.shareit.booking.repository;

import com.sun.jdi.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.Booking;

import java.util.*;

@Slf4j
@Repository("InMemoryBookingRepository")
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<Long, NavigableSet<Booking>> bookings = new HashMap<>(); // <K> itemId
    private final Map<Long, Long> bookingToItem = new HashMap<>();  // <K> BookingId, <V>itemId

    private Long nextId = 1L;

    @Override
    public Booking addBooking(Booking booking) {

        booking.setId(nextId++);
        bookingToItem.put(booking.getId(), booking.getItemId()); // Добавляем связь ID бронирования → ID вещи

        if (getItemBookings(booking.getItemId()).add(booking)) {
            return booking;
        }
        throw new InternalException("Ошибка при сохранении брони");
    }

    @Override
    public Booking patchBooking(Booking booking) {
        return findBooking(booking.getId())
                .map(existing -> {
                    existing.setStatus(booking.getStatus());
                    return existing;
                })
                .orElseThrow(() -> new InternalException(
                        "Бронирование с ID " + booking.getId() + " несмотря на проверку существования, не найдено"
                ));
    }

    @Override
    public void deleteBooking(Booking booking) {
        Long itemId = bookingToItem.get(booking.getId());

        NavigableSet<Booking> bookingsSet = bookings.get(itemId);
        bookingsSet.remove(booking);
        bookingToItem.remove(booking.getId());

        // Очищаем записи, если больше нет бронирований для этого item
        if (bookingsSet.isEmpty()) {
            bookings.remove(itemId);
        }
    }

    @Override
    public Optional<Booking> findBooking(Long bookingId) {
        Long itemId = bookingToItem.get(bookingId);
        if (itemId == null) {
            return Optional.empty();
        }
        NavigableSet<Booking> itemBookings = bookings.get(itemId);
        return itemBookings != null
                ? itemBookings.stream()
                .filter(b -> Objects.equals(b.getId(), bookingId))
                .findFirst()
                : Optional.empty();
    }

    @Override
    public NavigableSet<Booking> getItemBookings(Long itemId) {
        return bookings.computeIfAbsent(itemId, k -> createTreeSetByStart());
    }

    @Override
    public Collection<Booking> getCreatedBookings(Long id) {
        return bookings.values().stream()
                .flatMap(Set::stream)
                .filter(b -> Objects.equals(b.getBookerId(), id))
                .toList();
    }

    private NavigableSet<Booking> createTreeSetByStart() {

        return new TreeSet<>(
                Comparator.comparing(Booking::getStart)
        );
    }

    //для теста
    public void clear() {
        bookings.clear();
        bookingToItem.clear();
        nextId = 1L;
    }
}
