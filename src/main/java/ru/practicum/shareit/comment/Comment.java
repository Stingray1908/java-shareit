package ru.practicum.shareit.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Long id;
    private Long bookerId;
    private Long itemId;
    private String comment;
    private Boolean isTaskCompleted;
    private LocalDateTime createdAt;  // Дата создания
    private LocalDateTime updatedAt;  // Дата последнего обновления
}
