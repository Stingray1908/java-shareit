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

        BookingSendDto dto = new BookingSendDto();
        dto.setId(booking.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());
        dto.setStatus(booking.getStatus());
        return dto;
    }

    @Override
    public BookingReqDto toReqDto(Booking booking) {
        if (booking == null) return null;

        BookingReqDto dto = new BookingReqDto();
        dto.setItemId(booking.getItem().getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());
        return dto;
    }

    @Override
    public Booking toEntity(BookingReqDto dto) {
        if (dto == null) return null;

        Booking booking = new Booking();
        booking.setStart(dto.getStart());
        booking.setEnd(dto.getEnd());
        return booking;
    }
}
