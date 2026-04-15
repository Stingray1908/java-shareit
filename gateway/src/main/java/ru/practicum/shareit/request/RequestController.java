package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.constants.HttpHeader;
import ru.practicum.shareit.common.groups.OnUpdate;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestClient requestClient;

    @PostMapping
    public ResponseEntity<Object> create(
            @Valid @RequestBody ItemRequestReqDTO requestDto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Ворота: получен запрос на создание запроса от пользователя ID: {}, данные: {}",
                requestorId, requestDto);
        ResponseEntity<Object> result = requestClient.create(requestDto, requestorId);
        log.info("Ворота: ответ от сервера получен для создания запроса");
        return result;
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<Object> patchStatus(
            @Validated(OnUpdate.class) @RequestBody ItemRequestReqDTO requestDto,
            @Positive(message = "ID запроса должен быть положительным числом") @PathVariable Long requestId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Ворота: получен запрос на обновление статуса запроса ID: {} от пользователя ID: {}",
                requestId, requestorId);
        ResponseEntity<Object> result = requestClient.patchStatus(requestDto, requestId, requestorId);
        log.info("Ворота: статус запроса ID: {} успешно обновлён", requestId);
        return result;
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getRequestWithItems(
            @Positive(message = "ID запроса должен быть положительным числом") @PathVariable Long requestId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Ворота: получен запрос на получение запроса ID: {} пользователя ID: {}", requestId, requestorId);
        ResponseEntity<Object> result = requestClient.getRequestWithItems(requestId);
        log.info("Ворота: запрос ID: {} успешно получен", requestId);
        return result;
    }

    @GetMapping
    public ResponseEntity<Object> getMyRequestsWithItems(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Ворота: получен запрос на получение всех запросов пользователя ID: {}", requestorId);
        ResponseEntity<Object> result = requestClient.getMyRequestsWithItems(requestorId);
        log.info("Ворота: все запросы пользователя ID: {} успешно получены", requestorId);
        return result;
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getAllRequestsWithItems() {
        log.info("Ворота: получен запрос на получение всех публичных запросов");
        ResponseEntity<Object> result = requestClient.getAllRequestsWithItems();
        log.info("Ворота: все публичные запросы успешно получены");
        return result;
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Object> delete(
            @Positive(message = "ID запроса должен быть положительным числом") @PathVariable Long requestId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Ворота: получен запрос на удаление запроса ID: {} пользователя ID: {}",
                requestId, requestorId);
        ResponseEntity<Object> result = requestClient.delete(requestId, requestorId);
        log.info("Ворота: запрос ID: {} пользователя ID: {} успешно удалён",
                requestId, requestorId);
        return result;
    }
}
