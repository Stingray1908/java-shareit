package ru.practicum.shareit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.service.BookingJpaService;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.item.service.ItemJPAService;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;

@Configuration
public class TestServiceConfiguration {

    @Bean
    public UserMapper userMapper() {
        return new UserMapper();
    }

    @Bean
    public RequestMapper requestMapper() {
        return new RequestMapper();
    }

    @Bean
    public ItemMapper itemMapper() {
        return new ItemMapper();
    }

    @Bean
    public BookingMapper bookingMapper(){
        return new BookingMapper();
    }

    @Bean
    public UserJPAService userJPAService(UserJPARepository userRepository, UserMapper userMapper) {
        return new UserJPAService(userRepository, userMapper);
    }

    @Bean
    public RequestJpaService requestJpaService(
            RequestJpaRepository requestRepository,
            UserJPAService userJPAService,
            RequestMapper requestMapper) {
        return new RequestJpaService(requestRepository, userJPAService, requestMapper);
    }

    @Bean
    public ItemJPAService itemJPAService(
            ItemJPARepository itemRepository,
            ItemMapper itemMapper,
            UserJPAService userJPAService,
            RequestJpaService requestJpaService) {
        return new ItemJPAService(itemRepository, itemMapper, userJPAService, requestJpaService);
    }

    @Bean
    public BookingJpaService bookingJpaService(BookingMapper bookingMapper) {
        return new BookingJpaService(bookingMapper);
    }

}
