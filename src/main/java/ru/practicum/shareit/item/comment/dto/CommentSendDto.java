package ru.practicum.shareit.item.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CommentSendDto {
    private Long id;
    private String authorName;
    private Long itemId;
    private String text;
    private LocalDateTime created;   // Дата создания (передаётся клиенту)
    private LocalDateTime updated;   // Дата обновления (передаётся клиенту)
}
