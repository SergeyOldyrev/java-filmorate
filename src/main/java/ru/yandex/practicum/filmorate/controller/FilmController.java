package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
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

    @PostMapping
    public ResponseEntity<Film> add(@RequestBody Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Попытка создания фильма с пустым названием");
            return ResponseEntity.badRequest().build();
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Описание фильма превышает 200 символов");
            return ResponseEntity.badRequest().build();
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Дата релиза раньше 28.12.1895");
            return ResponseEntity.badRequest().build();
        }
        if (film.getDuration() <= 0) {
            log.warn("Продолжительность фильма не положительная");
            return ResponseEntity.badRequest().build();
        }

        films.add(film);
        log.info("Добавлен фильм с id={}, name={}", film.getId(), film.getName());
        return ResponseEntity.status(201).body(film);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Film> update(@PathVariable long id, @RequestBody Film film) {
        if (film.getId() != 0 && film.getId() != id) {
            log.warn("ID в теле ({}) не совпадает с id в пути ({})", film.getId(), id);
            return ResponseEntity.badRequest().build();
        }

        for (int i = 0; i < films.size(); i++) {
            if (films.get(i).getId() == id) {
                films.set(i, film);
                log.info("Обновлен фильм с id={}", id);
                return ResponseEntity.ok(film);
            }
        }
        log.warn("Фильм с id={} не найден для обновления", id);
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAll() {
        return ResponseEntity.ok(films);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getById(@PathVariable long id) {
        for (Film f : films) {
            if (f.getId() == id) {
                return ResponseEntity.ok(f);
            }
        }
        return ResponseEntity.notFound().build();
    }
}