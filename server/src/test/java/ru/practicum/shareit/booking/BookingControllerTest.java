package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingReqDto;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.HttpHeader;
import ru.practicum.shareit.common.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    private final Long bookerId = 1L;
    private final Long bookingId = 1L;

    @Test
    void addBooking_Success() throws Exception {
        BookingReqDto requestDto = new BookingReqDto();
        requestDto.setItemId(1L);
        requestDto.setStart(LocalDateTime.now().plusDays(1));
        requestDto.setEnd(LocalDateTime.now().plusDays(2));

        BookingSendDto responseDto = new BookingSendDto();
        responseDto.setId(bookingId);
        responseDto.setStatus(BookingStatus.WAITING);

        when(bookingService.create(any(BookingReqDto.class), eq(bookerId))).thenReturn(responseDto);

        // Настраиваем ObjectMapper для работы с LocalDateTime
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String jsonContent = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/bookings")
                        .header(HttpHeader.X_SHARER_USER_ID, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingService, times(1)).create(any(BookingReqDto.class), eq(bookerId));
    }




    @Test
    void approveOrRejectBooking_Success() throws Exception {
        BookingSendDto responseDto = new BookingSendDto();
        responseDto.setId(bookingId);
        responseDto.setStatus(BookingStatus.APPROVED);

        when(bookingService.approveOrRejectBooking(eq(bookingId), eq(true), eq(bookerId)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                .param("approved", "true")
                .header(HttpHeader.X_SHARER_USER_ID, bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService, times(1))
                .approveOrRejectBooking(eq(bookingId), eq(true), eq(bookerId));
    }

    @Test
    void getBooking_Success() throws Exception {
        BookingSendDto responseDto = new BookingSendDto();
        responseDto.setId(bookingId);
        responseDto.setStatus(BookingStatus.WAITING);

        when(bookingService.getByIdForBookerOrOwner(eq(bookingId), eq(bookerId)))
                .thenReturn(responseDto);

        mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                .header(HttpHeader.X_SHARER_USER_ID, bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingService, times(1)).getByIdForBookerOrOwner(eq(bookingId), eq(bookerId));
    }

    @Test
    void getBookings_Success() throws Exception {
        List<BookingSendDto> responseList = Collections.singletonList(new BookingSendDto());
        responseList.get(0).setId(bookingId);

        when(bookingService.getBookingsByState(eq(bookerId), eq("ALL")))
                .thenReturn(responseList);

        mockMvc.perform(get("/bookings")
                .header(HttpHeader.X_SHARER_USER_ID, bookerId)
                .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId));

        verify(bookingService, times(1)).getBookingsByState(eq(bookerId), eq("ALL"));
    }

    @Test
    void getOwnerBookings_Success() throws Exception {
        List<BookingSendDto> responseList = Collections.singletonList(new BookingSendDto());
        responseList.get(0).setId(bookingId);

        when(bookingService.getBookingsByOwnerState(eq(bookerId), eq("ALL")))
                .thenReturn(responseList);

        mockMvc.perform(get("/bookings/owner")
                .header(HttpHeader.X_SHARER_USER_ID, bookerId)
                .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId));

        verify(bookingService, times(1)).getBookingsByOwnerState(eq(bookerId), eq("ALL"));
    }

    @Test
    void approveOrRejectBooking_NotFound() throws Exception {
        when(bookingService.approveOrRejectBooking(eq(bookingId), eq(true), eq(bookerId)))
                .thenThrow(new NoSuchElementException("Бронь с Id: " + bookingId + " не найдена"));

        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                .param("approved", "true")
                .header(HttpHeader.X_SHARER_USER_ID, bookerId))
                .andExpect(status().isNotFound());
    }
}
