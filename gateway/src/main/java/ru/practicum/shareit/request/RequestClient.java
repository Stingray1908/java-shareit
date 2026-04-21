package ru.practicum.shareit.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;

@Service
public class RequestClient extends BaseClient {
    private static final String API_PREFIX = "/requests";

    @Autowired
    public RequestClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .build()
        );
    }

    public ResponseEntity<Object> create(ItemRequestReqDTO requestDto, Long requestorId) {
        return post("", requestorId, requestDto);
    }

    public ResponseEntity<Object> patchStatus(ItemRequestReqDTO requestDto,
                                              Long requestId, Long requestorId) {
        return patch("/" + requestId, requestorId, requestDto);
    }

    public ResponseEntity<Object> getRequestWithItems(Long requestId) {
        return get("/" + requestId);
    }

    public ResponseEntity<Object> getMyRequestsWithItems(Long requestorId) {
        return get("", requestorId);
    }

    public ResponseEntity<Object> getAllRequestsWithItems() {
        return get("/all");
    }

    public ResponseEntity<Object> delete(Long requestId, Long requestorId) {
        return delete("/" + requestId, requestorId);
    }
}
