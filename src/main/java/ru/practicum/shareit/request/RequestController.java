package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.common.constants.HttpHeader;
import ru.practicum.shareit.common.groups.OnUpdate;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.service.RequestService;

import java.util.List;

/**
 * REST‑контроллер для управления запросами на предоставление вещей.
 * Предоставляет API‑методы для создания, изменения статуса, получения и удаления запросов,
 * а также для просмотра списков запросов.
 * <p>
 * Основные правила бизнес‑логики:
 * - Создание запроса доступно только авторизованному пользователю (ID передаётся в заголовке X-Requestor-User-Id).
 * - При создании запроса автоматически устанавливается статус PENDING и фиксируется время создания.
 * - Изменение статуса запроса ограничено: пользователь не может установить статусы PENDING или RESPONDED.
 * - Нельзя менять статус запроса, если его текущий статус — CANCELLED или COMPLETED.
 * - Операции изменения и удаления доступны только владельцу запроса.
 * - Все операции с ID проверяют их валидность: ID должны быть положительными числами.
 * - Если запрос или пользователь не найдены, выбрасывается NoSuchElementException (404).
 * - При нарушении прав доступа или правил изменения статуса выбрасывается IllegalArgumentException (400).
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestService requestService;

    /**
     * Создаёт новый запрос от имени пользователя.
     * Проверяет существование пользователя по ID (передаётся в заголовке X-Requestor-User-Id).
     * При успешном создании возвращает ItemRequestSendDTO с присвоенным ID и временем создания.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestSendDTO create(
            @Valid
            @RequestBody ItemRequestReqDTO requestDto,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_REQUESTOR_USER_ID) Long requestorId) {

        log.info("Получён запрос на создание запроса от пользователя ID: {}, данные: {}",
                requestorId, requestDto);

        ItemRequestSendDTO result = requestService.create(requestDto, requestorId);
        log.info("Выполнен запрос на создание. Ответ клиенту: {}", result);
        return result;
    }

    /**
     * Частично обновляет статус существующего запроса (PATCH).
     * ID запроса передаётся в пути запроса, ID пользователя — в заголовке.
     * Проверяет права доступа (запрос должен принадлежать пользователю) и допустимость нового статуса.
     * Запрещено устанавливать статусы PENDING и RESPONDED, а также менять статус для CANCELLED и COMPLETED запросов.
     */
    @PatchMapping("/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemRequestSendDTO patchStatus(
            @Validated(OnUpdate.class)
            @RequestBody ItemRequestReqDTO requestDto,
            @Positive(message = "ID запроса должен быть положительным числом")
            @PathVariable Long requestId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_REQUESTOR_USER_ID) Long requestorId) {

        return requestService.patchStatus(requestDto, requestId, requestorId);
    }

    /**
     * Возвращает данные конкретного запроса по ID.
     * Если запрос не найден, выбрасывает NoSuchElementException.
     */
    @GetMapping("/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemRequestSendDTO getById(
            @Positive(message = "ID запроса должен быть положительным числом")
            @PathVariable Long requestId) {

        log.info("Получён запрос на получение запроса по ID: {}", requestId);
        ItemRequestSendDTO request = requestService.getById(requestId);
        log.info("Запрос по ID: {} успешно получен. Ответ клиенту: {}",
                requestId, request);
        return request;
    }

    /**
     * Возвращает все запросы текущего пользователя.
     * ID пользователя передаётся в заголовке X-Requestor-User-Id.
     * Если пользователь не найден, выбрасывает NoSuchElementException.
     * При отсутствии запросов возвращает пустой список.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<ItemRequestSendDTO> getAllByCurrentUser(
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_REQUESTOR_USER_ID) Long requestorId) {

        log.info("Получён запрос на получение всех запросов пользователя ID: {}", requestorId);
        List<ItemRequestSendDTO> requests = requestService.getAllByRequestorId(requestorId);
        log.info("Получено {} запросов для пользователя ID: {}", requests.size(), requestorId);
        return requests;
    }

    /**
     * Удаляет запрос по ID.
     * ID запроса передаётся в пути, ID пользователя — в заголовке.
     * Операция доступна только владельцу запроса.
     * Если запрос или пользователь не найдены, выбрасывает NoSuchElementException.
     * Статус ответа — NO_CONTENT (204) при успешном удалении.
     */
    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @Positive(message = "ID запроса должен быть положительным числом")
            @PathVariable Long requestId,
            @Positive(message = "ID пользователя должен быть положительным числом")
            @RequestHeader(HttpHeader.X_REQUESTOR_USER_ID) Long requestorId) {

        log.info("Получён запрос на удаление запроса ID: {} пользователя ID: {}",
                requestId, requestorId);

        requestService.deleteById(requestId, requestorId);
        log.info("Запрос ID: {} пользователя ID: {} успешно удалён",
                requestId, requestorId);
    }
}

