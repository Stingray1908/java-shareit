package ru.practicum.shareit.item.comment;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;
import ru.practicum.shareit.common.GenericMapper;

@Component
public class CommentMapper implements GenericMapper<Comment, CommentReqDto, CommentSendDto> {

    @Override
    public CommentSendDto toSendDto(Comment comment) {
        if (comment == null) return null;
        return new CommentSendDto(
                comment.getId(),
                comment.getBooker().getName(),
                comment.getItem().getId(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    @Override
    public CommentReqDto toReqDto(Comment comment) {
        if (comment == null) return null;
        CommentReqDto dto = new CommentReqDto();
        dto.setText(comment.getText());

        return dto;
    }

    @Override
    public Comment toEntity(CommentReqDto dto) {
        if (dto == null) return null;
        Comment comment = new Comment();
        comment.setText(dto.getText());

        return comment;
    }
}
