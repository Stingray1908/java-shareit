package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.service.UserService;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        UserReqDTO userReqDTO = new UserReqDTO("John Doe", "john@example.com");
        UserSendDTO expectedUser = new UserSendDTO(1L, "John Doe", "john@example.com");

        when(userService.create(any(UserReqDTO.class))).thenReturn(expectedUser);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\"}"))  // закрывающая скобка после content()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).create(any(UserReqDTO.class));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        Long userId = 1L;
        UserReqDTO userReqDTO = new UserReqDTO("Jane Doe", "jane@example.com");
        UserSendDTO updatedUser = new UserSendDTO(userId, "Jane Doe", "jane@example.com");

        when(userService.update(anyLong(), any(UserReqDTO.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\"}"))  // закрывающая скобка после content()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));


        verify(userService, times(1)).update(eq(userId), any(UserReqDTO.class));
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        Long userId = 1L;
        UserSendDTO user = new UserSendDTO(userId, "John Doe", "john@example.com");

        when(userService.getById(anyLong())).thenReturn(user);

        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).getById(eq(userId));
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() throws Exception {
        UserSendDTO user1 = new UserSendDTO(1L, "John Doe", "john@example.com");
        UserSendDTO user2 = new UserSendDTO(2L, "Jane Smith", "jane@example.com");
        Collection<UserSendDTO> users = Arrays.asList(user1, user2);

        when(userService.getAll()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Jane Smith"));

        verify(userService, times(1)).getAll();
    }

    @Test
    void deleteUser_shouldDeleteUser() throws Exception {
        Long userId = 1L;

        doNothing().when(userService).delete(anyLong());

        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).delete(eq(userId));
    }
}
