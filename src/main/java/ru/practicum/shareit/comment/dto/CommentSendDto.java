package ru.practicum.shareit.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentSendDto {
    private Long id;
    private Long bookerId;
    private Long itemId;
    private String comment;
    private Boolean isTaskCompleted;
    private LocalDateTime createdAt;   // Дата создания (передаётся клиенту)
    private LocalDateTime updatedAt;   // Дата обновления (передаётся клиенту)
}
