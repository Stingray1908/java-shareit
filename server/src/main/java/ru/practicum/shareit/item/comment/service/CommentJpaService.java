package ru.practicum.shareit.item.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.repository.CommentJpaRepository;

@Service
@RequiredArgsConstructor

public class CommentJpaService implements CommentService {

    private final CommentJpaRepository commentRepository;

    @Override
    public Comment saveInternal(Comment comment) {
        return commentRepository.save(comment);
    }
}
