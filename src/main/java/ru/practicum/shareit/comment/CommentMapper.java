package ru.practicum.shareit.comment;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.comment.dto.CommentReqDto;
import ru.practicum.shareit.comment.dto.CommentSendDto;
import ru.practicum.shareit.common.GenericMapper;

@Component
public class CommentMapper implements GenericMapper<Comment, CommentReqDto, CommentSendDto> {

    @Override
    public CommentSendDto toSendDto(Comment comment) {
        if (comment == null) return null;
        return new CommentSendDto(
                comment.getId(),
                comment.getBookerId(),
                comment.getItemId(),
                comment.getComment(),
                comment.getIsTaskCompleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    @Override
    public CommentReqDto toReqDto(Comment comment) {
        if (comment == null) return null;
        CommentReqDto dto = new CommentReqDto();
        dto.setComment(comment.getComment());
        dto.setIsTaskCompleted(comment.getIsTaskCompleted());
        return dto;
    }

    @Override
    public Comment toEntity(CommentReqDto dto) {
        if (dto == null) return null;
        Comment comment = new Comment();
        comment.setComment(dto.getComment());
        comment.setIsTaskCompleted(dto.getIsTaskCompleted());

        return comment;
    }
}
