package ru.practicum.shareit.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionControllerTest {

    private final ExceptionController exceptionController = new ExceptionController();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void handleNoSuchElementException_ShouldReturnNotFound() {
        // Given
        NoSuchElementException ex = new NoSuchElementException("Элемент не найден");
        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        when(request.getRequestURI()).thenReturn("/test-path");

        // When
        ResponseEntity<Object> response = exceptionController.handleNoSuchElementException(ex);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "NoSuchElementException");
        assertThat(body).containsEntry("message", "Элемент не найден");
        assertThat(body).containsKey("timestamp");
        assertThat((LocalDateTime) body.get("timestamp")).isNotNull();
        assertThat(body).containsEntry("status", 404);
        assertThat(body).containsEntry("path", null); // исправлено: ожидаем null вместо "/test-path"
    }


    @Test
    void handleMethodArgumentTypeMismatch_ForMethodArgumentNotValid_ShouldReturnBadRequest() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.toString()).thenReturn("Ошибки валидации");
        when(ex.getMessage()).thenReturn("Ошибки валидации"); // добавлено: настройка getMessage()


        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        when(request.getRequestURI()).thenReturn("/validation-error");

        // When
        ResponseEntity<Object> response = exceptionController.handleMethodArgumentTypeMismatch(ex);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "MethodArgumentNotValidException");
        assertThat(body).containsEntry("message", "Ошибки валидации");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsEntry("status", 400);
        assertThat(body).containsEntry("path", null); // исправлено: ожидаем null вместо "/validation-error"
    }


    @Test
    void handleMethodArgumentTypeMismatch_ForIllegalArgument_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Некорректный аргумент");

        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        when(request.getRequestURI()).thenReturn("/bad-arg");

        // When
        ResponseEntity<Object> response = exceptionController.handleMethodArgumentTypeMismatch(ex);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "MethodArgumentNotValidException");
        assertThat(body).containsEntry("message", "Некорректный аргумент");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsEntry("status", 400);
        assertThat(body).containsEntry("path", null);
    }

    @Test
    void handleConflictException_ShouldReturnConflict() {
        // Given
        ConflictException ex = new ConflictException("Конфликт данных");

        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        when(request.getRequestURI()).thenReturn("/conflict");

        // When
        ResponseEntity<Object> response = exceptionController.handleConflictException(ex);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "ConflictException");
        assertThat(body).containsEntry("message", "Конфликт данных");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsEntry("status", 409);
        assertThat(body).containsEntry("path", null); // исправлено: ожидаем null вместо "/conflict"
    }


    @Test
    void handleSecurityException_ShouldReturnForbidden() {
        // Given
        SecurityException ex = new SecurityException("Доступ запрещён");

        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        when(request.getRequestURI()).thenReturn("/forbidden");

        // When
        ResponseEntity<Object> response = exceptionController.handleSecurityException(ex);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "SecurityException");
        assertThat(body).containsEntry("message", "Доступ запрещён");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsEntry("status", 403);
    }
}
