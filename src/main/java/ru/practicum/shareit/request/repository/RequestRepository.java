package ru.practicum.shareit.request.repository;

import ru.practicum.shareit.request.ItemRequest;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с запросами на получение предметов.
 */
public interface RequestRepository {

    /**
     * Сохраняет новый запрос в хранилище.
     *
     * @param request сущность запроса для сохранения
     * @return сохранённая сущность запроса с присвоенным ID
     */
    ItemRequest save(ItemRequest request);

    /**
     * Обновляет статус запроса в хранилище.
     *
     * @param patching сущность запроса с новым статусом
     * @return обновлённая сущность запроса
     */
    ItemRequest patchStatus(ItemRequest patching);

    /**
     * Находит запрос по его идентификатору.
     *
     * @param requestId идентификатор запроса
     * @return Optional с сущностью запроса, если найден, или пустой Optional
     */
    Optional<ItemRequest> findByRequestId(Long requestId);

    /**
     * Находит все запросы, созданные указанным пользователем.
     *
     * @param userId идентификатор пользователя
     * @return список сущностей запросов пользователя
     */
    List<ItemRequest> findByRequestorId(Long userId);

    /**
     * Удаляет запрос из хранилища по его идентификатору.
     *
     * @param requestId идентификатор запроса для удаления
     */
    void deleteById(Long requestId);

    /**
     * Возвращает список всех запросов из хранилища.
     *
     * @return список всех сущностей запросов
     */
    List<ItemRequest> findAll();
}
