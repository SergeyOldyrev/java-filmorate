package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApplicationTests {
    @Autowired
    private TestRestTemplate rest;
    /**
     * Всё, что тест создал, удаляем после себя — стор живёт весь прогон.
     */
    private final List<Integer> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdIds.forEach(id -> rest.delete("/users/{id}", id));
        createdIds.clear();
    }

    // ---------- позитивные ----------
    @Test
    void shouldSetNameToLoginWhenNameIsEmpty() {
        User user = validUser();
        user.setName(null);
        User created = create(user);
        assertEquals(user.getLogin(), created.getName());
    }

    @Test
    void shouldSetNameToLoginWhenNameIsEmptyString() {
        User user = validUser();
        user.setName("");
        User created = create(user);
        assertEquals(user.getLogin(), created.getName());
    }


    // ---------- валидация: ровно одно поле испорчено ----------
    @Test
    void shouldReturnBadRequestWhenEmailIsEmpty() {
        User user = validUser();
        user.setEmail("");
        assertStatusBadRequest(post(user));
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsNull() {
        User user = validUser();
        user.setEmail(null);
        assertStatusBadRequest(post(user));
    }

    @Test
    void shouldReturnBadRequestWhenEmailHasNoAt() {
        User user = validUser();
        user.setEmail("test.com");
        assertStatusBadRequest(post(user));
    }

    @Test
    void shouldReturnBadRequestWhenLoginIsEmpty() {
        User user = validUser();
        user.setLogin("");
        assertStatusBadRequest(post(user));
    }

    @Test
    void shouldReturnBadRequestWhenLoginContainsSpace() {
        User user = validUser();
        user.setLogin("test user"); // email валиден — 400 может дать только логин
        assertStatusBadRequest(post(user));
    }

    @Test
    void shouldReturnBadRequestWhenBirthdayIsInFuture() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));
        assertStatusBadRequest(post(user));
    }

    // ---------- обновление ----------
    @Test
    void shouldReturnBadRequestAndNotModifyUserWhenIdMismatchInUpdate() {
        User created = create(validUser());
        String originalLogin = created.getLogin();
        String originalEmail = created.getEmail();
        User update = validUser();
        update.setId(Integer.MAX_VALUE); // не совпадает с id в пути update.setLogin("mismatch" + suffix());
        ResponseEntity<User> response = put(created.getId(), update);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        User reloaded = rest.getForObject("/users/{id}", User.class, created.getId());
        assertNotNull(reloaded);
        assertEquals(originalLogin, reloaded.getLogin(), "запись изменилась несмотря на 400");
        assertEquals(originalEmail, reloaded.getEmail(), "запись изменилась несмотря на 400");
    }

    // ---------- хелперы ----------
    private User validUser() {
        String s = suffix();
        User user = new User();
        user.setLogin("login" + s);
        user.setEmail("user" + s + "@test.com");
        user.setName("Name " + s);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private User create(User user) {
        ResponseEntity<User> response = post(user);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody(), "POST /users вернул пустое тело");
        assertNotNull(response.getBody().getId(), "POST /users не вернул id");
        createdIds.add(response.getBody().getId());
        return response.getBody();
    }

    private ResponseEntity<User> post(User user) {
        return rest.postForEntity("/users", user, User.class);
    }

    private ResponseEntity<User> put(int pathId, User body) {
        return rest.exchange("/users/{id}", HttpMethod.PUT, new HttpEntity<>(body), User.class, pathId);
    }

    private void assertStatusBadRequest(ResponseEntity<?> response) {
        // TestRestTemplate не бросает исключений, поэтому 500 не замаскируется
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
