package ru.practicum.shareit.booking;

import java.time.LocalDateTime;

public record BookingDates(LocalDateTime lastBooking, LocalDateTime nextBooking) {}
