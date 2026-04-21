package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.HttpHeader;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.RequestService;
import ru.practicum.shareit.user.dto.UserSendDTO;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequestController.class)
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void create_shouldReturnCreatedRequest() throws Exception {
        Long requestorId = 1L;
        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription("Description");

        UserSendDTO userSendDTO = new UserSendDTO(requestorId, "Test User", "test@example.com");

        ItemRequestSendDTO expectedRequest = new ItemRequestSendDTO();
        expectedRequest.setId(1L);
        expectedRequest.setDescription("Description");
        expectedRequest.setRequester(userSendDTO);
        expectedRequest.setCreated(now);
        expectedRequest.setStatus(RequestStatus.PENDING);

        when(requestService.create(any(ItemRequestReqDTO.class), anyLong())).thenReturn(expectedRequest);

        mockMvc.perform(post("/requests")
                        .header(HttpHeader.X_SHARER_USER_ID, requestorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(requestService, times(1)).create(any(ItemRequestReqDTO.class), eq(requestorId));
    }

    @Test
    void patchStatus_shouldReturnUpdatedRequest() throws Exception {
        Long requestId = 1L;
        Long requestorId = 2L;

        ItemRequestReqDTO requestDto = new ItemRequestReqDTO();
        requestDto.setDescription("Updated description");
        requestDto.setStatus(RequestStatus.RESPONDED);

        UserSendDTO userSendDTO = new UserSendDTO(requestorId, "Updated User", "updated@example.com");

        ItemRequestSendDTO updatedRequest = new ItemRequestSendDTO();
        updatedRequest.setId(requestId);
        updatedRequest.setDescription("Updated description");
        updatedRequest.setRequester(userSendDTO);
        updatedRequest.setCreated(now);
        updatedRequest.setStatus(RequestStatus.RESPONDED);

        when(requestService.patchStatus(any(ItemRequestReqDTO.class), anyLong(), anyLong()))
                .thenReturn(updatedRequest);

        mockMvc.perform(patch("/requests/{requestId}", requestId)
                        .header(HttpHeader.X_SHARER_USER_ID, requestorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("RESPONDED"));

        verify(requestService, times(1)).patchStatus(any(ItemRequestReqDTO.class), eq(requestId), eq(requestorId));
    }

    @Test
    void getRequestWithItems_shouldReturnRequest() throws Exception {
        Long requestId = 1L;

        UserSendDTO userSendDTO = new UserSendDTO(1L, "Requester", "requester@example.com");

        ItemShortDto itemShortDto = new ItemShortDto();
        itemShortDto.setId(100L);
        itemShortDto.setName("Item 1");
        itemShortDto.setOwnerId(1L);

        ItemRequestWithItemsDto request = new ItemRequestWithItemsDto();
        request.setId(requestId);
        request.setDescription("Test request");
        request.setRequester(userSendDTO);
        request.setCreated(now);
        request.setStatus(RequestStatus.PENDING);
        request.setItems(Collections.singletonList(itemShortDto));

        when(requestService.getByIdWithItems(anyLong())).thenReturn(request);

        mockMvc.perform(get("/requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.description").value("Test request"))
                .andExpect(jsonPath("$.requester.id").value(1L))
                .andExpect(jsonPath("$.items[0].id").value(100L));

        verify(requestService, times(1)).getByIdWithItems(eq(requestId));
    }

    @Test
    void getMyRequestsWithItems_shouldReturnAllUserRequests() throws Exception {
        Long requestorId = 1L;

        UserSendDTO userSendDTO1 = new UserSendDTO(1L, "User 1", "user1@example.com");

        ItemShortDto item1 = new ItemShortDto();
        item1.setId(101L);
        item1.setName("Item A");
        item1.setOwnerId(1L);

        ItemRequestWithItemsDto request1 = new ItemRequestWithItemsDto();
        request1.setId(1L);
        request1.setDescription("Desc 1");
        request1.setRequester(userSendDTO1);
        request1.setCreated(now);
        request1.setStatus(RequestStatus.PENDING);
        request1.setItems(Collections.singletonList(item1));

        UserSendDTO userSendDTO2 = new UserSendDTO(2L, "User 2", "user2@example.com");

        ItemShortDto item2 = new ItemShortDto();
        item2.setId(102L);
        item2.setName("Item B");
        item2.setOwnerId(2L);

        ItemRequestWithItemsDto request2 = new ItemRequestWithItemsDto();
        request2.setId(2L);
        request2.setDescription("Desc 2");
        request2.setRequester(userSendDTO2);
        request2.setCreated(now);
        request2.setStatus(RequestStatus.RESPONDED);
        request2.setItems(Collections.singletonList(item2));

        List<ItemRequestWithItemsDto> requests = Arrays.asList(request1, request2);

        when(requestService.getAllByRequestorIdWithItems(anyLong())).thenReturn(requests);

        mockMvc.perform(get("/requests")
                        .header(HttpHeader.X_SHARER_USER_ID, requestorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].description").value("Desc 1"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].description").value("Desc 2"))
                .andExpect(jsonPath("$[1].status").value("RESPONDED"));

        verify(requestService, times(1)).getAllByRequestorIdWithItems(eq(requestorId));
    }

    @Test
    void getAllRequestsWithItems_shouldReturnAllPublicRequests() throws Exception {
        UserSendDTO userSendDTO1 = new UserSendDTO(1L,"Public User 1", "public1@example.com");

        ItemShortDto publicItem1 = new ItemShortDto();
        publicItem1.setId(201L);
        publicItem1.setName("Public Item A");
        publicItem1.setOwnerId(1L);

        ItemRequestWithItemsDto publicRequest1 = new ItemRequestWithItemsDto();
        publicRequest1.setId(1L);
        publicRequest1.setDescription("Public Desc 1");
        publicRequest1.setRequester(userSendDTO1);
        publicRequest1.setCreated(now);
        publicRequest1.setStatus(RequestStatus.PENDING);
        publicRequest1.setItems(Collections.singletonList(publicItem1));

        UserSendDTO userSendDTO2 = new UserSendDTO(2L, "Public User 2", "public2@example.com");

        ItemShortDto publicItem2 = new ItemShortDto();
        publicItem2.setId(202L);
        publicItem2.setName("Public Item B");
        publicItem2.setOwnerId(2L);

        ItemRequestWithItemsDto publicRequest2 = new ItemRequestWithItemsDto();
        publicRequest2.setId(2L);
        publicRequest2.setDescription("Public Desc 2");
        publicRequest2.setRequester(userSendDTO2);
        publicRequest2.setCreated(now);
        publicRequest2.setStatus(RequestStatus.RESPONDED);
        publicRequest2.setItems(Collections.singletonList(publicItem2));

        // ИСПРАВЛЕНО: добавлены publicRequest1 и publicRequest2 вместо publicItem1 и publicItem2
        List<ItemRequestWithItemsDto> publicRequests = Arrays.asList(publicRequest1, publicRequest2);

        when(requestService.getAllWithItems()).thenReturn(publicRequests);

        mockMvc.perform(get("/requests/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].description").value("Public Desc 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].description").value("Public Desc 2"));

        verify(requestService, times(1)).getAllWithItems();
    }


    @Test
    void delete_shouldDeleteRequest() throws Exception {
        Long requestId = 1L;
        Long requestorId = 2L;

        doNothing().when(requestService).deleteById(anyLong(), anyLong());

        mockMvc.perform(delete("/requests/{requestId}", requestId)
                        .header(HttpHeader.X_SHARER_USER_ID, requestorId))
                .andExpect(status().isNoContent());

        verify(requestService, times(1)).deleteById(eq(requestId), eq(requestorId));
    }
}

