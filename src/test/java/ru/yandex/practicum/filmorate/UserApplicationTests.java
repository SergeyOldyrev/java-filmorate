package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(UserController.class)
class UserApplicationTests {

    @InjectMocks
    private UserController userController;

    @Test
    void shouldReturnBadRequestWhenEmailIsEmpty() {
        User user = new User();
        user.setLogin("test");
        user.setBirthday(LocalDate.now().minusYears(20));

        ResponseEntity<?> response = userController.add(user);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenEmailMissingAt() {
        User user = new User();
        user.setLogin("test");
        user.setEmail("test.com");
        user.setBirthday(LocalDate.now().minusYears(20));

        ResponseEntity<?> response = userController.add(user);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenLoginContainsSpace() {
        User user = new User();
        user.setLogin("test user");
        user.setEmail("test@test.com");
        user.setBirthday(LocalDate.now().minusYears(20));

        ResponseEntity<?> response = userController.add(user);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldSetNameToLoginWhenNameIsEmpty() {
        User user = new User();
        user.setLogin("test");
        user.setEmail("test@test.com");
        user.setBirthday(LocalDate.now().minusYears(20));
        user.setName("");

        ResponseEntity<User> response = userController.add(user);
        assertEquals("test", response.getBody().getName());
    }

    @Test
    void shouldReturnBadRequestWhenBirthdayIsInFuture() {
        User user = new User();
        user.setLogin("test");
        user.setEmail("test@test.com");
        user.setBirthday(LocalDate.now().plusDays(1));

        ResponseEntity<?> response = userController.add(user);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
