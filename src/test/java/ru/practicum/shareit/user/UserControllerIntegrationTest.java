package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.UserReqDTO;
import ru.practicum.shareit.user.dto.UserSendDTO;
import ru.practicum.shareit.user.repository.InMemoryUserRepository;
import ru.practicum.shareit.user.repository.UserJPARepository;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.user.service.UserServiceImpl;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private InMemoryUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.clear();
    }

    private String asJsonString(Object obj) throws Exception {
        return new ObjectMapper().writeValueAsString(obj);
    }

    @Test
    void testCreateUser_Success() throws Exception {
        UserReqDTO userReqDTO = new UserReqDTO("Test User", "test@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userReqDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test User")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    void testCreateUser_ValidationError() throws Exception {
        UserReqDTO invalidUser = new UserReqDTO("", "invalid-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        // Сначала создаём пользователя
        UserSendDTO createdUser = userService.create(new UserReqDTO("Original", "original@example.com"));

        UserReqDTO updateDTO = new UserReqDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setEmail("updated@example.com");

        mockMvc.perform(patch("/users/" + createdUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdUser.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.email", is("updated@example.com")));
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        UserReqDTO updateDTO = new UserReqDTO();
        updateDTO.setName("Non-existent");

        mockMvc.perform(patch("/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserById_Success() throws Exception {
        UserSendDTO createdUser = userService.create(new UserReqDTO("Find Me", "find@example.com"));

        mockMvc.perform(get("/users/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdUser.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Find Me")))
                .andExpect(jsonPath("$.email", is("find@example.com")));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllUsers_Success() throws Exception {
        userService.create(new UserReqDTO("User 1", "user1@example.com"));
        userService.create(new UserReqDTO("User 2", "user2@example.com"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("User 1")))
                .andExpect(jsonPath("$[1].name", is("User 2")));
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        UserSendDTO createdUser = userService.create(new UserReqDTO("Delete Me", "delete@example.com"));

        mockMvc.perform(delete("/users/" + createdUser.getId()))
                .andExpect(status().isNoContent());

        // Проверяем, что пользователь удалён
        mockMvc.perform(get("/users/" + createdUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser_NotFound() throws Exception {
        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testPatchUser_PartialUpdate() throws Exception {
        UserSendDTO createdUser = userService.create(new UserReqDTO("Partial Update", "partial@example.com"));

        UserReqDTO partialUpdate = new UserReqDTO();
        partialUpdate.setName("New Name Only");

        mockMvc.perform(patch("/users/" + createdUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name Only")))
                .andExpect(jsonPath("$.email", is("partial@example.com"))); // email не изменился
    }

    @Test
    void testCreateUser_DuplicateEmail() throws Exception {
        // Создаём первого пользователя
        userService.create(new UserReqDTO("First", "duplicate@example.com"));

        // Пытаемся создать второго с тем же email
        UserReqDTO duplicateUser = new UserReqDTO("Second", "duplicate@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(duplicateUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateUser_InvalidId() throws Exception {
        UserReqDTO updateDTO = new UserReqDTO();
        updateDTO.setName("Invalid ID");

        mockMvc.perform(patch("/users/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateDTO)))
                .andExpect(status().isBadRequest()); // @Positive сработает
    }

}
