package ru.practicum.shareit.common;

/**
 * Класс с константами HTTP‑заголовков, используемых в приложении.
 * Централизованное хранение позволяет избежать дублирования строковых литералов
 * и упрощает рефакторинг при изменении имён заголовков.
 */
public class HttpHeader {

    /**
     * Заголовок для передачи ID пользователя‑арендатора (booker) при операциях с бронированиями.
     * Используется в {@link ru.practicum.shareit.booking.BookingController}.
     */
    public static final String X_BOOKER_USER_ID = "X-Booker-User-Id";

    /**
     * Заголовок для передачи общего ID пользователя в запросах.
     * Используется в {@link ru.practicum.shareit.booking.BookingController} и
     */
    public static final String X_USER_ID = "X-User-Id";

    /**
     * Заголовок для передачи ID владельца вещи (owner) при операциях с вещами.
     * Используется в {@link ru.practicum.shareit.item.ItemController}.
     */
    public static final String X_SHARER_USER_ID = "X-Sharer-User-Id";

    /**
     * Заголовок для передачи ID инициатора запроса (requestor) при операциях с запросами на вещи.
     * Используется в {@link ru.practicum.shareit.request.RequestController}.
     */
    public static final String X_REQUESTOR_USER_ID = "X-Requestor-User-Id";

    // Приватный конструктор для предотвращения создания экземпляров класса
    private HttpHeader() {
    }
}
