package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

    @PostMapping
    public ResponseEntity<User> add(@Valid @RequestBody User user) {
        // 1. Ручная валидация: дата не должна быть в будущем
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения в будущем: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        // 2. Логика: если имя пустое, берем логин
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        // 3. Сохраняем
        user.setId(nextId++);
        users.add(user);
        log.info("Добавлен пользователь с id={}, login={}", user.getId(), user.getLogin());

        return ResponseEntity.status(201).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable long id, @Valid @RequestBody User user) {
        // Проверка совпадения ID
        if (user.getId() != 0 && user.getId() != id) {
            log.warn("ID в теле ({}) не совпадает с id в пути ({})", user.getId(), id);
            throw new ValidationException("ID в теле не совпадает с ID в пути");
        }

        // Ручная валидация даты
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения в будущем при обновлении: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        // Подстановка имени
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        // Поиск и обновление
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == id) {
                users.set(i, user);
                log.info("Обновлен пользователь с id={}", id);
                return ResponseEntity.ok(user);
            }
        }

        log.warn("Пользователь с id={} не найден для обновления", id);
        return ResponseEntity.notFound().build();
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