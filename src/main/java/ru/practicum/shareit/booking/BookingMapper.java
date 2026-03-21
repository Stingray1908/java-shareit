package ru.practicum.shareit.booking;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.common.GenericMapper;

@Component
public class BookingMapper implements GenericMapper<Booking, BookingReqDto, BookingSendDto> {

    @Override
    public BookingSendDto toSendDto(Booking booking) {
        if (booking == null) return null;

        return new BookingSendDto(
                booking.getId(),
                booking.getBookerId(),
                booking.getItemId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getStatus()
        );
    }

    @Override
    public BookingReqDto toReqDto(Booking booking) {
        if (booking == null) return null;

        return new BookingReqDto(
                booking.getId(),
                booking.getBookerId(),
                booking.getItemId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getStatus());
    }

    @Override
    public Booking toEntity(BookingReqDto dto) {
        if (dto == null) return null;

        return new Booking(
                dto.getId(),
                dto.getBookerId(),
                dto.getItemId(),
                dto.getStart(),
                dto.getEnd(),
                dto.getStatus());
    }
}
