package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final List<Film> films = new ArrayList<>();
    private int nextId = 1;

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Попытка создания фильма с пустым названием");
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Название фильма не может быть пустым"));
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Описание фильма превышает 200 символов");
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Описание не должно превышать 200 символов"));
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Дата релиза раньше 28.12.1895");
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Дата релиза должна быть не раньше 28.12.1895"));
        }

        if (film.getDuration() <= 0) {
            log.warn("Продолжительность фильма не положительная");
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Длительность фильма должна быть положительной"));
        }

        film.setId(nextId++);
        films.add(film);
        log.info("Добавлен фильм с id={}, name={}", film.getId(), film.getName());
        return ResponseEntity.status(201).body(film);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Film film) {
        // 1. Проверка совпадения ID в теле и пути
        if (film.getId() != 0 && film.getId() != id) {
            log.warn("ID в теле ({}) не совпадает с id в пути ({})", film.getId(), id);
            // Возвращаем JSON: {"error": "ID в теле не совпадает с ID в пути"}
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "ID в теле не совпадает с ID в пути"));
        }

        // 2. Поиск фильма
        Film existing = films.stream()
                .filter(f -> f.getId() == id)
                .findFirst()
                .orElse(null);

        if (existing == null) {
            log.warn("Фильм с id={} не найден для обновления", id);
            // ВАЖНО: Возвращаем Map, Spring превратит его в JSON: {"error": "Фильм не найден"}
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Фильм не найден"));
        }

        // 3. Копируем поля из входящего объекта в существующий
        existing.setName(film.getName());
        existing.setDescription(film.getDescription());
        existing.setReleaseDate(film.getReleaseDate());
        existing.setDuration(film.getDuration());

        // ID не меняем, он уже верный

        log.info("Обновлён фильм с id={}, name={}", id, existing.getName());
        return ResponseEntity.ok(existing);
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAll() {
        return ResponseEntity.ok(films);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable long id) {
        Film film = films.stream()
                .filter(f -> f.getId() == id)
                .findFirst()
                .orElse(null);

        if (film == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Фильм не найден"));
        }
        return ResponseEntity.ok(film);
    }
}