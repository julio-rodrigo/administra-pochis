# Users Module V1 - AdministraPochi

## Purpose

This document defines the initial `users` module for AdministraPochi and should remain as persistent implementation context for GitHub Copilot and future changes.

Architecture:

```text
Controller -> DTO -> Service -> Mapper -> Entity -> Repository -> PostgreSQL
```

Authentication/authorization is intentionally separate from the first CRUD iteration, but password storage is designed safely from the beginning.

## Project structure

```text
com.administrapochi
├── configurations
├── controllers
│   └── UserController.java
├── dtos
│   └── users
│       ├── UserCreateRequest.java
│       ├── UserUpdateRequest.java
│       └── UserResponse.java
├── mappers
│   └── UserMapper.java
├── models
│   └── User.java
├── repositories
│   └── UserRepository.java
├── services
│   ├── UserService.java
│   └── UserServiceImpl.java
└── AdministrapochiApplication.java
```

## Maven dependencies: MapStruct + Lombok

Use MapStruct `1.6.3` (stable). Add the version property:

```xml
<properties>
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
</properties>
```

Inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${org.mapstruct.version}</version>
</dependency>
```

Keep the existing Lombok dependency. Configure the annotation processors:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

If `${lombok.version}` is not currently defined, use the same Lombok version already configured in the project instead of introducing a conflicting second version.

Verify annotation processing with:

```bash
./mvnw clean compile
```

MapStruct generates implementations under `target/generated-sources/annotations`. Never edit generated classes manually.

---

## Database table

Existing PostgreSQL table:

```sql
CREATE TABLE users (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

| PostgreSQL | Java | Notes |
|---|---|---|
| `id` | `Integer` | Database-generated identity |
| `name` | `String` | Required |
| `username` | `String` | Required and unique |
| `password_hash` | `String` | Encoded password only |
| `active` | `Boolean` | Defaults to true |
| `created_at` | `Instant` | Creation instant |

---

# 1. User entity

Create `models/User.java`:

```java
package com.administrapochi.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

Avoid Lombok `@Data` on JPA entities because it also generates `equals`, `hashCode`, and `toString`, which can become problematic when relationships are added.

PostgreSQL owns the defaults for `active` and `created_at`. During the first persistence test, verify that Hibernate allows those database defaults to apply. If Hibernate explicitly inserts null values, initialize those fields in Java or add suitable lifecycle handling.

---

# 2. User repository

Create `repositories/UserRepository.java`:

```java
package com.administrapochi.repositories;

import com.administrapochi.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

---

# 3. User DTOs

Create the DTOs in `dtos/users/`.

## UserCreateRequest

```java
package com.administrapochi.dtos.users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {
    private String name;
    private String username;
    private String password;
}
```

The API receives `password`, never `passwordHash`.

## UserUpdateRequest

```java
package com.administrapochi.dtos.users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    private String name;
    private String username;
}
```

Only `name` and `username` can be edited through this request. `active` is changed by the delete operation, not by general edits. Password changes are intentionally excluded; add a dedicated password-change operation later.

## UserResponse

```java
package com.administrapochi.dtos.users;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class UserResponse {
    private Integer id;
    private String name;
    private String username;
    private Boolean active;
    private Instant createdAt;
}
```

`UserResponse` must never expose `password` or `passwordHash`.

---

# 4. User mapper - MapStruct

Create `mappers/UserMapper.java`:

```java
package com.administrapochi.mappers;

import com.administrapochi.dtos.users.*;
import com.administrapochi.models.User;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}
```

MapStruct owns structural DTO/entity conversion only. It must not hash passwords, access repositories, or implement business rules. The update mapping changes only `name` and `username`; it must not modify `active`.

`componentModel = "spring"` makes the generated mapper injectable as a Spring bean.

---

# 5. User service contract

Create `services/UserService.java`:

```java
package com.administrapochi.services;

import com.administrapochi.dtos.users.*;
import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    UserResponse findById(Integer id);
    UserResponse create(UserCreateRequest request);
    UserResponse update(Integer id, UserUpdateRequest request);
    void delete(Integer id);
}
```

Controllers exchange DTOs with the service. JPA entities do not become the external API contract.

---

# 6. PasswordEncoder configuration

Before creating real users, configure a password encoder. With Spring Security available, create a bean such as:

```java
@Configuration
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

The flow is:

```text
UserCreateRequest.password
        ↓
UserServiceImpl
        ↓
PasswordEncoder.encode(...)
        ↓
User.passwordHash
        ↓
PostgreSQL users.password_hash
```

MapStruct must deliberately ignore `passwordHash` when converting `UserCreateRequest` into `User`.

---

# 7. User service implementation

Create `services/UserServiceImpl.java`:

```java
package com.administrapochi.services;

import com.administrapochi.dtos.users.*;
import com.administrapochi.mappers.UserMapper;
import com.administrapochi.models.User;
import com.administrapochi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> findAll() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    @Override
    public UserResponse findById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse update(Integer id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getUsername() != null
                && !request.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void delete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }
}
```

Constructor injection is generated by Lombok `@RequiredArgsConstructor` for the three `final` dependencies.
Deleting a user is a soft delete: the row remains in the database and `active` is set to `false`.

---

# 8. User controller

Create `controllers/UserController.java`:

```java
package com.administrapochi.controllers;

import com.administrapochi.dtos.users.*;
import com.administrapochi.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Integer id,
            @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

# 9. REST API

| Method | Endpoint | Request | Response |
|---|---|---|---|
| `GET` | `/api/users` | none | `List<UserResponse>` |
| `GET` | `/api/users/{id}` | none | `UserResponse` |
| `POST` | `/api/users` | `UserCreateRequest` | `UserResponse` |
| `PUT` | `/api/users/{id}` | `UserUpdateRequest` | `UserResponse` |
| `DELETE` | `/api/users/{id}` | none | no body; marks the user inactive (`active = false`) |

Example create request:

```json
{
  "name": "Rodrigo",
  "username": "rodrigo",
  "password": "example-password"
}
```

Example response:

```json
{
  "id": 1,
  "name": "Rodrigo",
  "username": "rodrigo",
  "active": true,
  "createdAt": "2026-11-05T02:00:00Z"
}
```

The response must never contain `password` or `passwordHash`.

---

# 10. Error handling

The implementation uses domain exceptions and a global `@RestControllerAdvice`:

```text
UserNotFoundException
UsernameAlreadyExistsException
```

Validation failures return `400 Bad Request` with field-level details in a Problem Details response.

| Situation | HTTP status |
|---|---|
| Created | `201 Created` |
| Read/update successful | `200 OK` |
| Delete successful | `204 No Content` |
| User not found | `404 Not Found` |
| Duplicate username | `409 Conflict` |
| Invalid input | `400 Bad Request` |

---

# 11. Validation

Jakarta Bean Validation is applied to request DTOs:

- `name`: not blank, maximum 100 characters.
- `username`: not blank, maximum 50 characters.
- `password`: not blank. Add a stronger policy when the application's requirements are defined.
- On update, only `name` and `username` are accepted. Both are optional; when supplied, they cannot be blank and must fit the column lengths.
- `active` cannot be changed through the edit request; deleting a user sets it to `false`.

Controller request bodies are annotated with `@Valid`.

Do not rely only on DTO validation for username uniqueness. PostgreSQL's `UNIQUE` constraint remains the final integrity guarantee.

---

# 12. Implementation order

1. Add/configure MapStruct dependencies and annotation processors.
2. Run `./mvnw clean compile`.
3. Create `User` entity.
4. Create `UserRepository`.
5. Create `UserCreateRequest`, `UserUpdateRequest`, and `UserResponse`.
6. Create `UserMapper`.
7. Compile again and confirm `UserMapperImpl` is generated.
8. Create `UserService`.
9. Configure `PasswordEncoder`.
10. Create `UserServiceImpl`.
11. Create `UserController`.
12. Start Spring Boot and verify Hibernate maps the existing `users` table.
13. Test `GET /api/users`.
14. Test user creation only after password encoding is configured.
15. Add Bean Validation and domain-specific exception handling.

---

# 13. Architecture decisions

- Database, Java code, DTOs and API naming use English.
- PostgreSQL table is `users`.
- Primary key is `INTEGER GENERATED ALWAYS AS IDENTITY`.
- Java ID type is `Integer`.
- PostgreSQL `TIMESTAMPTZ` maps to Java `Instant`.
- `username` is unique.
- Deleting a user is a soft delete that sets `active` to `false`; the database row is retained.
- User edits can change only `name` and `username`.
- Passwords are never stored in plain text.
- Only encoded passwords are stored in `password_hash`.
- Lombok reduces Java boilerplate.
- MapStruct handles DTO/entity mappings at compile time.
- MapStruct mappers use `componentModel = "spring"`.
- MapStruct never performs password hashing or business logic.
- Controllers expose DTOs, not JPA entities.
- `UserService` is the service contract.
- `UserServiceImpl` implements business rules and orchestration.
- `UserRepository` owns database access through Spring Data JPA.
- Authentication/authorization will be implemented separately.
- Password changes will use a dedicated operation instead of the generic user update DTO.

---

# 14. GitHub Copilot context

Use this context when implementing or changing the module:

```text
Project: AdministraPochi
Framework: Spring Boot
Persistence: Spring Data JPA / Hibernate
Database: PostgreSQL
Boilerplate: Lombok
Mapping: MapStruct
Base package: com.administrapochi
Architecture: Controller -> DTO -> Service interface -> Service implementation -> Mapper -> Entity -> Repository -> PostgreSQL
Date/time: TIMESTAMPTZ in PostgreSQL, Instant in Java
IDs: INTEGER identity in PostgreSQL, Integer in Java
Passwords: plain password exists only in request processing; password_hash stores only PasswordEncoder output
API: entities are never returned directly; use UserResponse; delete is soft (`active=false`)
User updates: only `name` and `username` are editable; `active` changes only through delete
Mapping: MapStruct maps DTOs/entities, but never hashes passwords or performs business logic
Source-code naming: English
```

## Next planned iteration

After this module compiles and CRUD works:

1. Add mapper, service and controller tests.
2. Add a dedicated password-change operation.
3. Add authentication only after user creation and password storage are verified.

Update this document whenever the database schema, DTO contracts, mapper behavior, service rules, endpoints, or security decisions change.
