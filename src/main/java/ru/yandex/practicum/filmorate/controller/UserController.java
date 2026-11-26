package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final List<User> users = new ArrayList<>();
    private Long nextId = 1L;

    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения в будущем: {}", user.getBirthday());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Дата рождения не может быть в будущем"));
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Логин не может быть пустым"));
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        if (user.getLogin().contains(" ")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Логин не может содержать пробелы"));
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email не может быть пустым"));
        }
        if (!user.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email должен содержать символ @"));
        }
        user.setId(nextId++);
        users.add(user);
        log.info("Добавлен пользователь с id={}, login={}", user.getId(), user.getLogin());

        return ResponseEntity.status(201).body(user);
    }

    @PutMapping
    public ResponseEntity<?> update(@Valid @RequestBody User user) {
        if (user.getId() == null) {
            log.warn("ID пользователя не указан");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID должен быть указан"));
        }

        User existing = users.stream()
                .filter(u -> Objects.equals(u.getId(), user.getId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            log.warn("Пользователь с id={} не найден", user.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пользователь не найден"));
        }

        // Не забудьте также перенести проверки валидации из add() (логин без пробелов, дата рождения не в будущем и т.д.)
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Дата рождения не может быть в будущем"));
        }
        if (user.getLogin() != null) {
            if (user.getLogin().isBlank() || user.getLogin().contains(" ")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Логин не может быть пустым или содержать пробелы"));
            }
            existing.setLogin(user.getLogin());
        }
        if (user.getEmail() != null) {
            if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Некорректный email"));
            }
            existing.setEmail(user.getEmail());
        }
        if (user.getName() != null) {
            existing.setName(user.getName().isBlank() ? existing.getLogin() : user.getName());
        }
        if (user.getBirthday() != null) {
            existing.setBirthday(user.getBirthday());
        }

        log.info("Обновлён пользователь с id={}, login={}", existing.getId(), existing.getLogin());
        return ResponseEntity.ok(existing);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable long id) {
        User user = users.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("Запрос несуществующего пользователя с id={}", id);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пользователь не найден"));
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        User existing = users.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);

        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пользователь не найден"));
        }

        users.remove(existing);
        log.info("Удален пользователь с id={}", id);

        return ResponseEntity.noContent().build();
    }
}