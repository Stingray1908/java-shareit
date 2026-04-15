package ru.practicum.shareit.item.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.comment.dto.CommentSendDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CommentMapperTest {

    @Autowired
    private CommentMapper commentMapper;

    private User booker;
    private Item item;
    private Comment comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        createdAt = LocalDateTime.now().minusDays(1);
        updatedAt = LocalDateTime.now();

        booker = new User();
        booker.setId(1L);
        booker.setName("Test User");

        item = new Item();
        item.setId(2L);

        comment = new Comment();
        comment.setId(3L);
        comment.setBooker(booker);
        comment.setItem(item);
        comment.setText("Test comment text");
        comment.setCreatedAt(createdAt);
        comment.setUpdatedAt(updatedAt);
    }

    @Test
    void toSendDto_ShouldMapAllFieldsCorrectly() {
        // When
        CommentSendDto result = commentMapper.toSendDto(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getAuthorName()).isEqualTo("Test User");
        assertThat(result.getItemId()).isEqualTo(2L);
        assertThat(result.getText()).isEqualTo("Test comment text");
        assertThat(result.getCreated()).isEqualTo(createdAt);
        assertThat(result.getUpdated()).isEqualTo(updatedAt);
    }

    @Test
    void toSendDto_ShouldReturnNull_WhenCommentIsNull() {
        // When
        CommentSendDto result = commentMapper.toSendDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toReqDto_ShouldMapTextFieldCorrectly() {
        // When
        CommentReqDto result = commentMapper.toReqDto(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo("Test comment text");
    }

    @Test
    void toReqDto_ShouldReturnNull_WhenCommentIsNull() {
        // When
        CommentReqDto result = commentMapper.toReqDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toEntity_ShouldMapTextFieldCorrectly() {
        // Given
        CommentReqDto dto = new CommentReqDto();
        dto.setText("Entity test text");

        // When
        Comment result = commentMapper.toEntity(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo("Entity test text");
        assertThat(result.getId()).isNull();
        assertThat(result.getBooker()).isNull();
        assertThat(result.getItem()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_ShouldReturnNull_WhenDtoIsNull() {
        // When
        Comment result = commentMapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }
}

