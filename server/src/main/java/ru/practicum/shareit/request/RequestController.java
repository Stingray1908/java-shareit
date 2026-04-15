package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.practicum.shareit.common.HttpHeader;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.RequestService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestSendDTO create(
            @RequestBody ItemRequestReqDTO requestDto,
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Сервер: получен запрос на создание запроса от пользователя ID: {}, данные: {}",
                requestorId, requestDto);
        ItemRequestSendDTO result = requestService.create(requestDto, requestorId);
        log.info("Сервер: выполнен запрос на создание. Ответ клиенту: {}", result);
        return result;
    }

    @PatchMapping("/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemRequestSendDTO patchStatus(
            @RequestBody ItemRequestReqDTO requestDto,
            @PathVariable Long requestId,
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        return requestService.patchStatus(requestDto, requestId, requestorId);
    }

    @GetMapping("/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemRequestWithItemsDto getRequestWithItems(@PathVariable Long requestId) {
        log.info("Сервер: получен запрос на получение запроса по ID: {}", requestId);
        ItemRequestWithItemsDto request = requestService.getByIdWithItems(requestId);
        log.info("Сервер: запрос по ID: {} успешно получен. Ответ клиенту: {}",
                requestId, request);
        return request;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<ItemRequestWithItemsDto> getMyRequestsWithItems(
            @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Сервер: получен запрос на получение всех запросов пользователя ID: {}", requestorId);
        List<ItemRequestWithItemsDto> requests = requestService.getAllByRequestorIdWithItems(requestorId);
        log.info("Сервер: получено {} запросов для пользователя ID: {}", requests.size(), requestorId);
        return requests;
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<ItemRequestWithItemsDto> getAllRequestsWithItems() {
        log.info("Сервер: получен запрос на получение всех публичных запросов");
        List<ItemRequestWithItemsDto> requests = requestService.getAllWithItems();
        log.info("Сервер: получено {} публичных запросов", requests.size());
        return requests;
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long requestId,
                @RequestHeader(HttpHeader.X_SHARER_USER_ID) Long requestorId) {
        log.info("Сервер: получен запрос на удаление запроса ID: {} пользователя ID: {}",
                requestId, requestorId);
        requestService.deleteById(requestId, requestorId);
        log.info("Сервер: запрос ID: {} пользователя ID: {} успешно удалён",
                requestId, requestorId);
    }
}
