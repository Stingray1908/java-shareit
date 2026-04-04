package ru.practicum.shareit.item.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.comment.Comment;


import java.util.List;

@Repository
 public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByItemId(Long itemId);

    boolean existsByBookerIdAndItemId(Long bookerId, Long itemId);
}
