/*package ru.practicum.shareit.comment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.item.comment.CommentMapper;
import ru.practicum.shareit.comment.dto.CommentReqDto;
import ru.practicum.shareit.comment.dto.CommentSendDto;
import ru.practicum.shareit.comment.repository.CommentRepository;
import ru.practicum.shareit.common.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final BookingServiceImpl bookingService;
    private final CommentMapper mapper;

    public CommentServiceImpl(CommentRepository commentRepository,
                              BookingServiceImpl bookingService,
                              CommentMapper mapper) {
        this.commentRepository = commentRepository;
        this.bookingService = bookingService;
        this.mapper = mapper;
    }

    @Override
    public CommentSendDto createComment(CommentReqDto reqDto, Long bookerId, Long itemId) {
        // Проверяем, что пользователь уже бронировал эту вещь и бронирование завершено
        validateBookingCompleted(bookerId, itemId);

        // Проверяем, что ещё нет отзыва на эту вещь от этого пользователя
        if (commentRepository.existsByBookerAndItem(bookerId, itemId)) {
            throw new IllegalArgumentException("Пользователь уже оставил отзыв на эту вещь");
        }

        Comment comment = mapper.toEntity(reqDto);
        comment.setBookerId(bookerId);
        comment.setItemId(itemId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment created = commentRepository.addComment(comment);
        log.info("Отзыв создан для вещи ID: {}, пользователем ID: {}", itemId, bookerId);
        return mapper.toSendDto(created);
    }

    @Override
    public CommentSendDto updateComment(Long commentId, CommentReqDto reqDto, Long bookerId) {
        Comment existingComment = commentRepository.getComment(commentId)
                .orElseThrow(() -> new NoSuchElementException("Отзыв с ID: " + commentId + " не найден"));

        // Проверяем, что обновляет автор
        if (!Objects.equals(existingComment.getBookerId(), bookerId)) {
            throw new SecurityException("Только автор может редактировать свой отзыв");
        }

        existingComment.setComment(reqDto.getComment());
        existingComment.setIsTaskCompleted(reqDto.getIsTaskCompleted());
        existingComment.setUpdatedAt(LocalDateTime.now()); // Обновляем дату последнего изменения

        log.info("Отзыв ID: {} обновлён пользователем ID: {}", commentId, bookerId);
        return mapper.toSendDto(existingComment);
    }

    @Override
    public void deleteComment(Long commentId, Long bookerId) {
        Comment comment = commentRepository.getComment(commentId)
                .orElseThrow(() -> new NoSuchElementException("Отзыв с ID: " + commentId + " не найден"));

        // Проверяем, что удаляет автор
        if (!Objects.equals(comment.getBookerId(), bookerId)) {
            throw new SecurityException("Попытка пользователя ID: " + bookerId +
                    " удалить комментарий ID: " + commentId + " пользователя ID: " + comment.getBookerId());
        }
        commentRepository.deleteComment(commentId);
        log.info("Отзыв ID: {} удалён пользователем ID: {}", commentId, bookerId);
    }

    @Override
    public Collection<CommentSendDto> getCommentsByItemId(Long itemId) {
        List<Comment> comments = commentRepository.getCommentsByItemId(itemId);
        return comments.stream()
                .map(mapper::toSendDto)
                .collect(Collectors.toList());
    }


    private void validateBookingCompleted(Long bookerId, Long itemId) {
        Collection<Booking> bookings = bookingService.getItemBookingsInternal(itemId);

        boolean hasCompletedBooking = bookings.stream()
                .filter(booking -> Objects.equals(booking.getBookerId(), bookerId))
                .anyMatch(booking -> booking.getStatus() == BookingStatus.COMPLETED);

        if (!hasCompletedBooking) {
            throw new IllegalArgumentException(
                    "Пользователь не имеет завершённого бронирования (статус COMPLETED) для этой вещи"
            );
        }
    }
}
*/
