package ru.practicum.shareit.request.repository;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.request.ItemRequest;

import java.util.List;
import java.util.Optional;

/**
 * JPA‑репозиторий для работы с сущностями запросов на аренду предметов (ItemRequest) в базе данных.
 * Расширяет стандартный JpaRepository Spring Data JPA, добавляя специализированные методы для бизнес‑логики работы с запросами.
 *
 * <p>
 * Основные функции:
 * - CRUD‑операции (наследуются от JpaRepository);
 * - сохранение и удаление запросов на аренду;
 * - поиск запросов по ID (в т. ч. с фильтрацией по статусу);
 * - получение списка всех запросов конкретного пользователя (requester).
 * <p>
 * Ключевые методы:
 * - save — сохраняет новый или обновлённый запрос на аренду;
 * - findByIdWithActiveStatus — находит запрос по ID, но только если его статус — PENDING или RESPONDED;
 * - findById — находит запрос по ID без фильтрации по статусу;
 * - findAllByRequesterId — возвращает все запросы, созданные указанным пользователем;
 * - deleteById — удаляет запрос по его ID.
 * <p>
 * Особенности реализации:
 * - использует аннотации @Query для написания кастомных JPQL‑запросов;
 * - применяет JOIN FETCH для загрузки связанных сущностей (например, requester) за один запрос — это снижает количество обращений к БД;
 * - возвращает Optional/List для безопасной обработки результатов;
 * - интегрируется с транзакционным менеджментом Spring через @Transactional в сервисе;
 * - аннотации @NonNull гарантируют отсутствие null‑значений в ключевых параметрах и результатах.
 */
public interface RequestJpaRepository extends JpaRepository<ItemRequest, Long> {

    /**
     * Сохраняет новый или обновляет существующий запрос на аренду предмета.
     * Гарантирует, что передаваемый объект запроса не является null.
     *
     * @param request объект запроса на аренду, который нужно сохранить
     * @return сохранённый объект ItemRequest с присвоенным ID
     */
    @SuppressWarnings("unchecked")
    @NonNull
    ItemRequest save(@NonNull ItemRequest request);

    /**
     * Находит запрос на аренду по его уникальному идентификатору, но только если статус запроса — PENDING или RESPONDED.
     * Загружает связанного пользователя (requester) с помощью JOIN FETCH.
     * Используется для сценариев, где нужны только активные запросы.
     *
     * @param id ID запроса на аренду
     * @return Optional с сущностью запроса, если найден и его статус подходит; пустой Optional — если не найден или статус не соответствует
     */
    @Query("SELECT r FROM ItemRequest r JOIN FETCH r.requester WHERE r.id = :id AND r.status IN ('PENDING', 'RESPONDED')")
    Optional<ItemRequest> findByIdWithActiveStatus(@Param("id") Long id);

    /**
     * Находит запрос на аренду по его уникальному идентификатору без фильтрации по статусу.
     * Загружает связанного пользователя (requester) с помощью JOIN FETCH.
     * Подходит для сценариев, где нужен полный доступ к данным запроса независимо от его статуса.
     *
     * @param id ID запроса на аренду
     * @return Optional с сущностью запроса, если найден; пустой Optional — если не найден
     */

    @Query("SELECT r FROM ItemRequest r JOIN FETCH r.requester WHERE r.id = :id")
    Optional<ItemRequest> findById(@NonNull Long id);

    /**
     * Возвращает список всех запросов на аренду, созданных указанным пользователем (requester).
     * Загружает связанных пользователей (requester) для каждого запроса с помощью JOIN FETCH.
     * Используется, например, для отображения истории запросов пользователя в личном кабинете.
     *
     * @param requesterId ID пользователя, чьи запросы нужно получить
     * @return список сущностей ItemRequest, созданных указанным пользователем; пустой список — если запросов нет
     */
    @Query("SELECT r FROM ItemRequest r JOIN FETCH r.requester WHERE r.requester.id = :requesterId")
    List<ItemRequest> findAllByRequesterId(@Param("requesterId") Long requesterId);

    /**
     * Удаляет запрос на аренду по его уникальному идентификатору.
     * Если запрос с указанным ID не существует, операция игнорируется (не вызывает ошибок).
     *
     * @param id ID запроса на аренду для удаления
     */
    void deleteById(Long id);
}
