package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final List<User> users = new ArrayList<>();

    @PostMapping
    public ResponseEntity<User> add(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Попытка создания пользователя с некорректным email: {}", user.getEmail());
            return ResponseEntity.badRequest().build();
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Попытка создания пользователя с некорректным логином: {}", user.getLogin());
            return ResponseEntity.badRequest().build();
        }
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя не указано, устанавливаем логин как имя: {}", user.getLogin());
            user.setName(user.getLogin());
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Попытка создания пользователя с датой рождения в будущем: {}", user.getBirthday());
            return ResponseEntity.badRequest().build();
        }

        users.add(user);
        log.info("Добавлен пользователь с id={}, login={}", user.getId(), user.getLogin());
        return ResponseEntity.status(201).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable long id, @RequestBody User user) {
        if (user.getId() != 0 && user.getId() != id) {
            log.warn("ID в теле ({}) не совпадает с id в пути ({})", user.getId(), id);
            return ResponseEntity.badRequest().build();
        }

        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Попытка обновления пользователя с некорректным email: {}", user.getEmail());
            return ResponseEntity.badRequest().build();
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Попытка обновления пользователя с некорректным логином: {}", user.getLogin());
            return ResponseEntity.badRequest().build();
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Попытка обновления пользователя с датой рождения в будущем: {}", user.getBirthday());
            return ResponseEntity.badRequest().build();
        }

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