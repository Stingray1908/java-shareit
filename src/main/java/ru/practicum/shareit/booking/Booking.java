        package ru.practicum.shareit.booking;

        import jakarta.persistence.*;
        import lombok.AllArgsConstructor;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        import lombok.NoArgsConstructor;

        import ru.practicum.shareit.common.enums.BookingStatus;
        import ru.practicum.shareit.item.Item;
        import ru.practicum.shareit.user.User;

        import java.time.LocalDateTime;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @EqualsAndHashCode(exclude = {"start", "end", "status"})
        @Entity
        @Table(name = "bookings")
        public class Booking {
            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            @ManyToOne(fetch = FetchType.LAZY, optional = false)
            @JoinColumn(name = "booker_id", nullable = false)
            private User booker;

            @ManyToOne(fetch = FetchType.LAZY, optional = false)
            @JoinColumn(name = "item_id", nullable = false)
            private Item item;

            @Column(nullable = false)
            private LocalDateTime start;

            @Column(name = "\"end\"", nullable = false)
            private LocalDateTime end;

            @Column(nullable = false)
            @Enumerated(EnumType.STRING)
            private BookingStatus status;

        }
