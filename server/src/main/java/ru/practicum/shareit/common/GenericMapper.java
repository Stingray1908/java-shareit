package ru.practicum.shareit.common;

/**
 * Универсальный интерфейс маппера для преобразования между сущностями и DTO.
 * Обеспечивает единый контракт для всех мапперов в проекте.
 *
 * @param <E>        тип сущности (Entity)
 * @param <R>  тип DTO для запросов (Request DTO)
 * @param <D> тип DTO для ответов (Send/Response DTO)
 */
public interface GenericMapper<E, R, D> {

    /**
     * Преобразует сущность в DTO для отправки клиенту (ответ API)
     *
     * @param entity исходная сущность (может быть null)
     * @return DTO для отправки или null, если entity == null
     */
    D toSendDto(E entity);

    /**
     * Преобразует сущность в DTO для передачи между сервисами (запрос на обновление/создание)
     *
     * @param entity исходная сущность (может быть null)
     * @return DTO для запросов или null, если entity == null
     */
    R toReqDto(E entity);

    /**
     * Преобразует DTO запроса в сущность для сохранения/обновления в БД
     *
     * @param dto исходный DTO (может быть null)
     * @return сущность или null, если dto == null
     */
    E toEntity(R dto);
}
