package ru.practicum.shareit.item.comment.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentReqDto {
    @Size(max = 1000, message = "Комментарий не может превышать 1000 символов")
    private String text;
}
