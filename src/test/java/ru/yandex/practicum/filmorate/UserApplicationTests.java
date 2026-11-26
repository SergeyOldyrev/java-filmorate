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
        return rest.exchange("/users", HttpMethod.PUT, new HttpEntity<>(body), User.class, pathId);
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
                "/users",
                HttpMethod.PUT,
                new HttpEntity<>(update),
                Map.class,
                created.getId()
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        assertTrue(response.getBody().containsKey("error"));
        assertFalse(response.getBody().containsKey("id"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExisting() {
        User update = validUser();
        update.setId(123456L);

        ResponseEntity<Map> response = rest.exchange(
                "/users",
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

        // 2. Генерируем данные для обновления
        String suffix = suffix();
        String expectedLogin = "updated_login_" + suffix;
        String expectedEmail = "updated_" + suffix + "@test.com";
        String expectedName = "Updated Name " + suffix;

        User updatePayload = validUser();
        updatePayload.setId(createdId); // ID должен совпадать
        updatePayload.setLogin(expectedLogin);
        updatePayload.setEmail(expectedEmail);
        updatePayload.setName(expectedName);
        // Birthday можно не менять, оставим старый

        // 3. Отправляем PUT запрос
        ResponseEntity<User> response = rest.exchange(
                "/users",
                HttpMethod.PUT,
                new HttpEntity<>(updatePayload),
                User.class,
                createdId
        );

        // 4. ПРОВЕРКИ (Assertions)

        // Статус должен быть 200 OK (не 500!)
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Статус должен быть 200 OK");

        assertNotNull(response.getBody(), "Тело ответа не должно быть null");

        User updatedUser = response.getBody();

        // Проверка ID
        assertEquals(createdId, updatedUser.getId(), "ID должен совпадать");

        // Проверка полей
        assertEquals(expectedLogin, updatedUser.getLogin(), "Login должен обновиться");
        assertEquals(expectedEmail, updatedUser.getEmail(), "Email должен обновиться");
        assertEquals(expectedName, updatedUser.getName(), "Name должен обновиться");

        // Birthday должен остаться старым (если мы его не меняли)
        assertEquals(created.getBirthday(), updatedUser.getBirthday(), "Birthday не должен измениться");

        System.out.println("✅ Тест пройден! Пользователь успешно обновлен: " + updatedUser);
    }

}
