package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApplicationTests {
    @LocalServerPort
    private int port;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder().rootUri("http://localhost:" + port).build();
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsEmpty() {
        User user = new User();
        user.setLogin("test_login"); // email пустой -> сработает @NotBlank в модели
        try {
            restTemplate.postForEntity("/users", user, User.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode()); // Можно проверить текст ошибки, если нужно: //
            assertTrue(e.getResponseBodyAsString().contains("Email обязателен"));
        }
    }

    @Test
    void shouldReturnBadRequestWhenEmailMissingAt() {
        User user = new User();
        user.setLogin("test_login");
        user.setEmail("test.com"); // нет @
        try {
            restTemplate.postForEntity("/users", user, User.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnBadRequestWhenLoginContainsSpace() {
        User user = new User();
        user.setLogin("test user"); // пробел user.setEmail("test@test.com");
        try {
            restTemplate.postForEntity("/users", user, User.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldSetNameToLoginWhenNameIsEmpty() {
        User user = new User();
        user.setLogin("my_login");
        user.setEmail("test@test.com"); // name пустой
        ResponseEntity<User> response = restTemplate.postForEntity("/users", user, User.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("my_login", response.getBody().getName()); // Имя подставилось из логина
    }

    @Test
    void shouldReturnBadRequestWhenBirthdayIsInFuture() {
        User user = new User();
        user.setLogin("test_login");
        user.setEmail("test@test.com");
        user.setBirthday(LocalDate.now().plusDays(1)); // Завтра
        try {
            restTemplate.postForEntity("/users", user, User.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnBadRequestWhenIdMismatchInUpdate() {
        // 1. Создаем пользователя
        User existing = new User();
        existing.setLogin("existing_login");
        existing.setEmail("existing@test.com");
        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", existing, User.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        long id = createResponse.getBody().getId();

        // 2. Пытаемся обновить с несовпадающим ID в теле
        User update = new User();
        update.setId(999); // Не совпадает с ID в пути
        update.setLogin("updated_login");
        update.setEmail("updated@test.com");

        HttpEntity<User> entity = new HttpEntity<>(update);

        try {
            // ПРАВИЛЬНЫЙ ВЫЗОВ: exchange + HttpMethod.PUT
            restTemplate.exchange(
                    "/users/{id}",
                    HttpMethod.PUT,
                    entity,
                    User.class,
                    id
            );
            fail("Ожидалась ошибка 400 из-за несовпадения ID");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingUser() {
        User update = new User();
        update.setId(999);
        update.setLogin("non_exist_login");
        update.setEmail("non_exist@test.com");

        HttpEntity<User> entity = new HttpEntity<>(update);

        try {
            // Пытаемся обновить несуществующего пользователя
            restTemplate.exchange(
                    "/users/{id}",
                    HttpMethod.PUT,
                    entity,
                    User.class,
                    999L
            );
            fail("Ожидалась ошибка 404");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
    @Test
    void shouldReturnOkWhenUpdatingValidUser() {
        // 1. Создаем пользователя
        User existing = new User();
        existing.setLogin("old_login");
        existing.setEmail("old@test.com");
        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", existing, User.class);
        long id = createResponse.getBody().getId();

        // 2. Обновляем корректными данными
        User update = new User();
        update.setId((int) id); // ID совпадает с путем
        update.setLogin("new_login");
        update.setEmail("new@test.com");
        update.setName("New Name");

        HttpEntity<User> entity = new HttpEntity<>(update);

        ResponseEntity<User> updateResponse = restTemplate.exchange(
                "/users/{id}",
                HttpMethod.PUT,
                entity,
                User.class,
                id
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("new_login", updateResponse.getBody().getLogin());
        assertEquals("New Name", updateResponse.getBody().getName());
    }
}
