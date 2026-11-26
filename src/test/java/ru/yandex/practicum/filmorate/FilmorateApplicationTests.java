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
import ru.yandex.practicum.filmorate.model.Film;


import java.time.LocalDate;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FilmorateApplicationTests {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldReturnBadRequestWhenNameIsEmpty() {
        Film film = new Film();
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(90);
        try {
            restTemplate.postForEntity("/films", film, Film.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnBadRequestWhenDurationNegative() {
        Film film = new Film();
        film.setName("Valid name");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(-5);
        try {
            restTemplate.postForEntity("/films", film, Film.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnCreatedWhenFilmIsValid() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Short description");
        film.setReleaseDate(LocalDate.of(1896, 1, 1));
        film.setDuration(120);
        ResponseEntity<Film> response = restTemplate.postForEntity("/films", film, Film.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() {
        Film existing = new Film();
        existing.setName("Existing Film");
        existing.setDescription("Old description");
        existing.setReleaseDate(LocalDate.now());
        existing.setDuration(90);
        ResponseEntity<Film> createResponse = restTemplate.postForEntity("/films", existing, Film.class);
        long id = createResponse.getBody().getId();
        Film update = new Film();
        update.setId(id);
        update.setName("Updated");
        update.setDescription("A".repeat(201));
        update.setReleaseDate(LocalDate.of(1895, 12, 27));
        update.setDuration(-10);
        HttpEntity<Film> updateEntity = new HttpEntity<>(update);
        try {
            restTemplate.exchange("/films", HttpMethod.PUT, updateEntity, String.class, id);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnBadRequestWhenReleaseDateBefore1895() {
        Film film = new Film();
        film.setName("Valid name");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(90);
        try {
            restTemplate.postForEntity("/films", film, Film.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }

    }

    @Test
    void shouldReturnBadRequestWhenDescriptionExceeds200Chars() {
        Film film = new Film();
        film.setName("Valid name");
        film.setDescription("A".repeat(201)); // 201 символ
        film.setReleaseDate(LocalDate.now());
        film.setDuration(90);

        try {
            restTemplate.postForEntity("/films", film, Film.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnBadRequestWhenDurationZero() {
        Film film = new Film();
        film.setName("Valid name");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(0);

        try {
            restTemplate.postForEntity("/films", film, Film.class);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnBadRequestWhenIdMismatchInUpdate() {
        // 1. Создаем фильм
        Film existing = new Film();
        existing.setId(1L);
        existing.setName("Existing");
        existing.setDescription("Desc");
        existing.setReleaseDate(LocalDate.now());
        existing.setDuration(90);

        HttpEntity<Film> createEntity = new HttpEntity<>(existing);
        try {
            restTemplate.exchange("/films", HttpMethod.POST, createEntity, String.class);
        } catch (HttpClientErrorException e) {
            fail("Не удалось создать фильм: " + e.getStatusCode());
        }

        // 2. Пытаемся обновить с несовпаданием ID
        Film update = new Film();
        update.setId(2L);
        update.setName("Updated");
        update.setDescription("New desc");
        update.setReleaseDate(LocalDate.now());
        update.setDuration(120);

        HttpEntity<Film> updateEntity = new HttpEntity<>(update);

        try {
            restTemplate.exchange("/films", HttpMethod.PUT, updateEntity, String.class, 1L);
            fail("Ожидалась ошибка 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingFilm() {
        Film update = new Film();
        update.setId(999L);
        update.setName("Updated");
        update.setDescription("Desc");
        update.setReleaseDate(LocalDate.now());
        update.setDuration(90);

        HttpEntity<Film> updateEntity = new HttpEntity<>(update);

        try {
            restTemplate.exchange("/films", HttpMethod.PUT, updateEntity, String.class, 999L);
            fail("Ожидалась ошибка 404");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}
