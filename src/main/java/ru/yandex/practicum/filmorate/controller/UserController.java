package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения в будущем: {}", user.getBirthday());

            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Дата рождения не может быть в будущем"));
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(nextId++);
        users.add(user);
        log.info("Добавлен пользователь с id={}, login={}", user.getId(), user.getLogin());

        return ResponseEntity.status(201).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody User user) {

        if (user.getId() != 0 && user.getId() != id) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID в теле не совпадает с ID в пути"));
        }

        User existing = users.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);

        if (existing == null) {
            log.warn("Пользователь с id={} не найден", id);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пользователь не найден"));
        }

        existing.setName(user.getName());

        return ResponseEntity.ok(existing);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable long id) {
        for (User u : users) {
            if (u.getId() == id) {
                return ResponseEntity.ok(u);
            }
        }
        log.warn("Запрос несуществующего пользователя с id={}", id);
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(users);
    }
}