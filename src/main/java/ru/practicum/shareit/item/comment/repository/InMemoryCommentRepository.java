/*package ru.practicum.shareit.item.comment.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.comment.Comment;

import java.util.*;

@Repository
public class InMemoryCommentRepository implements CommentRepository {

    private final Map<Long, Comment> comments = new HashMap<>(); // <id comment, comment>
    private final Map<Long, List<Long>> itemComments = new HashMap<>(); // <id item, list<id comment>>
    private Long nextId = 1L;

    @Override
    public Comment addComment(Comment comment) {
        Long id = nextId++;
        comment.setId(id);
        comments.put(id, comment);

        itemComments.computeIfAbsent(comment.getItem().getId(), k -> new ArrayList<>())
                .add(id);
        return comment;
    }

    @Override
    public Optional<Comment> getComment(Long commentId) {
        return Optional.ofNullable(comments.get(commentId));
    }

    @Override
    public void deleteComment(Long commentId) {
        Comment comment = comments.remove(commentId);
        if (comment != null) {
            List<Long> commentIds = itemComments.get(comment.getItem().getId());
            if (commentIds != null) {
                commentIds.remove(Long.valueOf(commentId)); // Явное указание типа для корректного удаления по значению
            }
        }
    }

    @Override
    public List<Comment> getCommentsByItemId(Long itemId) {
        List<Long> commentIds = itemComments.getOrDefault(itemId, Collections.emptyList());
        List<Comment> result = new ArrayList<>(commentIds.size());
        for (Long commentId : commentIds) {
            Comment comment = comments.get(commentId);
            if (comment != null) {
                result.add(comment);
            }
        }
        return result;
    }

    @Override
    public boolean existsByBookerAndItem(Long bookerId, Long itemId) {
        List<Long> commentIds = itemComments.get(itemId);
        if (commentIds == null || commentIds.isEmpty()) {
            return false;
        }

        for (Long commentId : commentIds) {
            Comment comment = comments.get(commentId);
            if (comment != null && Objects.equals(comment.getBooker().getId(), bookerId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteAll() {

    }

    public void clear() {
        comments.clear();
        itemComments.clear();
        nextId = 1L;
    }
}
*/

