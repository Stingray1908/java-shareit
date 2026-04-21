package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.comment.dto.CommentReqDto;
import ru.practicum.shareit.item.dto.ItemReqDTO;

@Service
public class ItemClient extends BaseClient {
    private static final String API_PREFIX = "/items";

    @Autowired
    public ItemClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .build()
        );
    }

    public ResponseEntity<Object> create(ItemReqDTO itemReqDTO, Long ownerId) {
        return post("", ownerId, itemReqDTO);
    }

    public ResponseEntity<Object> addComment(Long itemId, CommentReqDto dto, Long userId) {
        return post("/" + itemId + "/comment", userId, dto);
    }

    public ResponseEntity<Object> update(Long itemId, Long userId, ItemReqDTO itemReqDTO) {
        return patch("/" + itemId, userId, itemReqDTO);
    }

    public ResponseEntity<Object> getById(Long id) {
        return get("/" + id);
    }

    public ResponseEntity<Object> getOwnerItems(Long ownerId) {
        return get("", ownerId);
    }

    public ResponseEntity<Object> getRequestItemsById(Long id, Long ownerId) {
        return get("/request/" + id, ownerId);
    }

    public ResponseEntity<Object> deleteByItemAndOwnerIds(Long userId, Long itemId) {
        return delete("/" + itemId, userId);
    }

    public ResponseEntity<Object> search(String text) {
        return get("/search?text=" + text);
    }
}
