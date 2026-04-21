package ru.practicum.shareit.item.comment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.repository.CommentJpaRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentJpaServiceTest {

    @Mock
    private CommentJpaRepository commentRepository;

    @InjectMocks
    private CommentJpaService commentService;

    @Test
    void testSaveInternal_ShouldSaveCommentAndReturnSavedComment() {
        // Arrange: подготавливаем тестовые данные и задаём поведение мока
        Comment inputComment = new Comment();
        inputComment.setId(null); // ID ещё нет — это новый комментарий
        inputComment.setText("Отличный предмет!");

        Comment savedComment = new Comment();
        savedComment.setId(1L); // После сохранения репозиторий присвоит ID
        savedComment.setText("Отличный предмет!");

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // Act: вызываем тестируемый метод
        Comment result = commentService.saveInternal(inputComment);

        // Assert: проверяем корректность результата и взаимодействие с репозиторием
        assertEquals(savedComment, result, "Метод должен вернуть сохранённый комментарий с присвоенным ID");
        assertEquals(1L, result.getId(), "ID сохранённого комментария должен быть равен 1");
        verify(commentRepository).save(inputComment);
    }
}
