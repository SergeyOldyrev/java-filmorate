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
import java.util.Map;
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

    @Test
    void shouldReturnBadRequestAndNotModifyUserWhenIdMismatchInUpdate() {
        User created = create(validUser());
        String originalLogin = created.getLogin();
        String originalEmail = created.getEmail();
        User update = validUser();
        update.setId((long) Integer.MAX_VALUE); // не совпадает с id в пути update.setLogin("mismatch" + suffix());
        ResponseEntity<User> response = put(Math.toIntExact(created.getId()), update);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        User reloaded = rest.getForObject("/users/{id}", User.class, created.getId());
        assertNotNull(reloaded);
        assertEquals(originalLogin, reloaded.getLogin(), "запись изменилась несмотря на 400");
        assertEquals(originalEmail, reloaded.getEmail(), "запись изменилась несмотря на 400");
    }

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
        createdIds.add(Math.toIntExact(response.getBody().getId()));
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

    @Test
    void shouldReturnBadRequestWhenIdMismatch() {
        // Создаем пользователя
        User created = create(validUser());

        // Готовим обновление с другим ID
        User update = validUser();
        update.setId(99999L); // Несовпадающий ID
        update.setLogin("new_login");

        ResponseEntity<Map> response = rest.exchange(
                "/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(update),
                Map.class,
                created.getId()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertTrue(response.getBody().containsKey("error"));
        assertFalse(response.getBody().containsKey("id"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExisting() {
        User update = validUser();
        update.setId(123456L);

        ResponseEntity<Map> response = rest.exchange(
                "/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(update),
                Map.class,
                123456L
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    void shouldReturnOkAndUpdateFields() {
        // 1. Создаем пользователя
        User created = create(validUser());
        long createdId = created.getId();

        // 2. Генерируем уникальный суффикс ОДИН РАЗ
        String suffix = suffix();
        String expectedLogin = "updated_login_" + suffix;
        String expectedEmail = "updated_" + suffix + "@test.com";
        String expectedName = "Updated Name " + suffix;

        // 3. Готовим данные для обновления, используя тот же суффикс
        User update = validUser();
        update.setId(createdId);
        update.setLogin(expectedLogin);
        update.setEmail(expectedEmail);
        update.setName(expectedName);

        // 4. Отправляем PUT запрос
        ResponseEntity<User> response = rest.exchange(
                "/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(update),
                User.class,
                createdId
        );

        // 5. Проверяем статус
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // 6. Сравниваем поля с заранее сохраненными значениями
        assertEquals(expectedLogin, response.getBody().getLogin());
        assertEquals(expectedEmail, response.getBody().getEmail());
        assertEquals(expectedName, response.getBody().getName());
        assertEquals(createdId, response.getBody().getId());
    }
}
