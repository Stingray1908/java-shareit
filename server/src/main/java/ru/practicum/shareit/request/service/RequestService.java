package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Сервис для управления запросами на получение предметов.
 */
public interface RequestService {

    /**
     * Создаёт новый запрос на получение предмета от пользователя.
     *
     * @param requestDto  DTO с данными для создания запроса
     * @param requestorId идентификатор пользователя, создающего запрос
     * @return DTO с данными созданного запроса
     * @throws NoSuchElementException если пользователь с указанным ID не найден
     */
    ItemRequestSendDTO create(ItemRequestReqDTO requestDto, Long requestorId);

    /**
     * Обновляет статус существующего запроса.
     *
     * @param requestDto  DTO с новыми данными статуса запроса
     * @param requestId   идентификатор запроса, который нужно обновить
     * @param requestorId идентификатор пользователя — инициатора запроса
     * @return DTO с данными обновлённого запроса
     * @throws IllegalArgumentException если:
     *                                  - новый статус недопустим (PENDING или RESPONDED);
     *                                  - текущий статус запроса не позволяет его изменить (CANCELLED или COMPLETED)
     * @throws NoSuchElementException   если запрос с указанным ID не найден
     */
    ItemRequestSendDTO patchStatus(ItemRequestReqDTO requestDto, Long requestId, Long requestorId);

    /**
     * Внутреннее обновление статуса запроса (используется внутри системы).
     *
     * @param request сущность запроса с обновлённым статусом
     * @return обновлённая сущность запроса
     */
    ItemRequest patchStatusInternal(ItemRequest request);

    /**
     * Получает сущность запроса по его идентификатору для внутреннего использования.
     *
     * @param requestId идентификатор запроса
     * @return сущность запроса
     * @throws NoSuchElementException если запрос с указанным ID не найден
     */
    ItemRequest findActiveRequestByIdOrThrowInternal(Long requestId);

    /**
     * Получает DTO запроса по его идентификатору для внешнего использования (клиента).
     *
     * @param requestId идентификатор запроса
     * @return DTO с данными запроса
     * @throws NoSuchElementException если запрос с указанным ID не найден
     */
    ItemRequestSendDTO getById(Long requestId);

    /**
     * Возвращает список запросов, созданных указанным пользователем.
     *
     * @param requestorId идентификатор пользователя — инициатора запросов
     * @return список DTO с запросами пользователя
     * @throws NoSuchElementException если пользователь с указанным ID не зарегистрирован в системе
     */
    List<ItemRequestSendDTO> getAllByRequestorId(Long requestorId);

    /**
     * Удаляет запрос по его идентификатору.
     *
     * @param requestId   идентификатор запроса, который нужно удалить
     * @param requesterId идентификатор пользователя — владельца запроса
     * @throws IllegalArgumentException если запрос не принадлежит указанному пользователю
     * @throws NoSuchElementException   если запрос с указанным ID не найден
     */
    void deleteById(Long requestId, Long requesterId);

    ItemRequest findRequestByIdOrThrowInternal(Long id);

    List<ItemRequestWithItemsDto> getAllByRequestorIdWithItems(Long requestorId);

    List<ItemRequestWithItemsDto> getAllWithItems();

    ItemRequestWithItemsDto getByIdWithItems(Long id);

}
