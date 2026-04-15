    package ru.practicum.shareit.booking.dto;

    import lombok.Data;
    import ru.practicum.shareit.common.enums.BookingStatus;
    import ru.practicum.shareit.item.dto.ItemSendDTO;
    import ru.practicum.shareit.user.dto.UserSendDTO;

    import java.time.LocalDateTime;

    @Data
    public class BookingSendDto {
        private Long id;
        private UserSendDTO booker;
        private ItemSendDTO item;
        private LocalDateTime start;
        private LocalDateTime end;
        private BookingStatus status;
    }
