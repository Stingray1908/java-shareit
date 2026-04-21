package ru.practicum.shareit.item.comment.service;

import ru.practicum.shareit.item.comment.Comment;

public interface CommentService {

    Comment saveInternal(Comment comment);
}
