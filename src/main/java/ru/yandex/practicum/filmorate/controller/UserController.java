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
        // 1. ЗАЩИТА: Если тело запроса пустое, сразу возвращаем ошибку, а не ломаемся
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Тело запроса не может быть пустым"));
        }

        // 2. Проверка ID (опционально, зависит от требований)
        if (user.getId() != 0 && user.getId() != id) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID в теле не совпадает с ID в пути"));
        }

        // 3. Поиск пользователя в списке
        User existing = users.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);

        // 4. Если пользователь не найден -> 404
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пользователь не найден"));
        }

        // 5. Явное обновление полей (КОПИРОВАНИЕ ДАННЫХ)
        // Это самый надежный способ, он не сломается, если в запросе пропущено какое-то поле
        if (user.getLogin() != null) existing.setLogin(user.getLogin());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getName() != null) existing.setName(user.getName());
        if (user.getBirthday() != null) existing.setBirthday(user.getBirthday());

        log.info("Обновлен пользователь с id={}, login={}", id, existing.getLogin());

        // 6. Возвращаем УСПЕШНЫЙ ответ (200 OK) с полным объектом
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
}