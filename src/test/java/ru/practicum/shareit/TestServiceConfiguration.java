/*package ru.practicum.shareit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingJpaRepository;
import ru.practicum.shareit.booking.service.BookingJpaService;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.comment.CommentRepository;
import ru.practicum.shareit.item.repository.ItemJPARepository;
import ru.practicum.shareit.item.service.ItemJPAService;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.request.service.RequestJpaService;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;

@Configuration
@Profile("test")
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
            UserMapper userMapper,
            RequestJpaService requestJpaService
    ,RequestMapper requestMapper,
            CommentRepository commentRepository,
            BookingJpaRepository bookingRepository) {
        return new ItemJPAService(itemRepository, itemMapper, userJPAService, userMapper, requestJpaService, requestMapper,
                commentRepository,
                bookingRepository);
    }

    @Bean
    public BookingJpaService bookingJpaService(BookingJpaRepository bookingRepository, UserService userService, ItemService itemService) {
        return new BookingJpaService(bookingRepository, userService, itemService);
    }

}
*/
