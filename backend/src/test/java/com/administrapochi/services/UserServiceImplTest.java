package com.administrapochi.services;

import com.administrapochi.dtos.users.UserCreateRequest;
import com.administrapochi.dtos.users.UserResponse;
import com.administrapochi.dtos.users.UserUpdateRequest;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.exceptions.UsernameAlreadyExistsException;
import com.administrapochi.mappers.UserMapper;
import com.administrapochi.models.User;
import com.administrapochi.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findAllReturnsUsersMappedToResponses() {
        List<User> users = List.of(existingUser());
        List<UserResponse> responses = List.of(responseFor(users.getFirst()));
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponseList(users)).thenReturn(responses);

        List<UserResponse> result = userService.findAll();

        assertSame(responses, result);
        verify(userRepository).findAll();
        verify(userMapper).toResponseList(users);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void findByIdReturnsMappedUser() {
        User user = existingUser();
        UserResponse response = responseFor(user);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.findById(1);

        assertSame(response, result);
        verify(userRepository).findById(1);
        verify(userMapper).toResponse(user);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void findByIdThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class, () -> userService.findById(1));

        assertEquals("User with id 1 was not found", exception.getMessage());
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void createPersistsUserAndReturnsMappedSavedUser() {
        UserCreateRequest request = createRequest();
        User user = User.builder().name(request.getName()).username(request.getUsername()).build();
        User savedUser = existingUser();
        UserResponse response = responseFor(savedUser);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertSame(response, result);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(savedUser);
    }

    @Test
    void createThrowsUsernameAlreadyExistsExceptionWithoutPersistingUser() {
        UserCreateRequest request = createRequest();
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        UsernameAlreadyExistsException exception = assertThrows(
                UsernameAlreadyExistsException.class, () -> userService.create(request));

        assertEquals("Username 'original' already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void createEncodesPasswordBeforePersistingUser() {
        UserCreateRequest request = createRequest();
        User user = User.builder().name(request.getName()).username(request.getUsername()).build();
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(user)).thenAnswer(invocation -> {
            User persistedUser = invocation.getArgument(0);
            assertEquals("encoded-password", persistedUser.getPasswordHash());
            return persistedUser;
        });
        when(userMapper.toResponse(user)).thenReturn(responseFor(user));

        userService.create(request);

        var ordered = inOrder(passwordEncoder, userRepository);
        ordered.verify(passwordEncoder).encode(request.getPassword());
        ordered.verify(userRepository).save(user);
        assertEquals("encoded-password", user.getPasswordHash());
    }

    @Test
    void updateDelegatesNameAndUsernameChangesToMapperAndPreservesProtectedFields() {
        User user = existingUser();
        user.setActive(false);
        UserUpdateRequest request = updateRequest("Updated name", "updated");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("updated")).thenReturn(false);
        doAnswer(invocation -> {
            UserUpdateRequest changes = invocation.getArgument(0);
            User target = invocation.getArgument(1);
            target.setName(changes.getName());
            target.setUsername(changes.getUsername());
            return null;
        }).when(userMapper).updateEntity(request, user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenAnswer(invocation -> responseFor(user));

        UserResponse result = userService.update(1, request);

        assertAll(
                () -> assertEquals("Updated name", result.getName()),
                () -> assertEquals("updated", result.getUsername()),
                () -> assertEquals(1, user.getId()),
                () -> assertFalse(user.getActive()),
                () -> assertEquals("existing-hash", user.getPasswordHash()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), user.getCreatedAt()));
        var ordered = inOrder(userMapper, userRepository);
        ordered.verify(userMapper).updateEntity(request, user);
        ordered.verify(userRepository).save(user);
        ordered.verify(userMapper).toResponse(user);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateThrowsUsernameAlreadyExistsExceptionWhenAnotherUserUsesUsername() {
        User user = existingUser();
        UserUpdateRequest request = updateRequest("Updated name", "taken");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        UsernameAlreadyExistsException exception = assertThrows(
                UsernameAlreadyExistsException.class, () -> userService.update(1, request));

        assertAll(
                () -> assertEquals("Username 'taken' already exists", exception.getMessage()),
                () -> assertEquals("Original name", user.getName()),
                () -> assertEquals("original", user.getUsername()));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void updateThrowsUserNotFoundExceptionWithoutUpdatingOrPersisting() {
        UserUpdateRequest request = updateRequest("Updated name", "updated");
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class, () -> userService.update(1, request));

        assertEquals("User with id 1 was not found", exception.getMessage());
        verify(userRepository).findById(1);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void updateDoesNotCheckUniquenessWhenUsernameIsUnchanged() {
        User user = existingUser();
        UserUpdateRequest request = updateRequest("Updated name", "original");
        UserResponse response = responseFor(user);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1, request);

        assertSame(response, result);
        verify(userRepository, never()).existsByUsername(any());
        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
    }

    @Test
    void updateDoesNotCheckUniquenessWhenUsernameIsOmitted() {
        User user = existingUser();
        UserUpdateRequest request = updateRequest("Updated name", null);
        UserResponse response = responseFor(user);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1, request);

        assertSame(response, result);
        verify(userRepository, never()).existsByUsername(any());
        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
    }

    @Test
    void deleteMarksUserInactiveAndSavesWithoutPhysicallyDeleting() {
        User user = existingUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenAnswer(invocation -> {
            User persistedUser = invocation.getArgument(0);
            assertSame(user, persistedUser);
            assertFalse(persistedUser.getActive());
            return persistedUser;
        });

        userService.delete(1);

        assertAll(
                () -> assertFalse(user.getActive()),
                () -> assertEquals(1, user.getId()),
                () -> assertEquals("Original name", user.getName()),
                () -> assertEquals("original", user.getUsername()),
                () -> assertEquals("existing-hash", user.getPasswordHash()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), user.getCreatedAt()));
        verify(userRepository).findById(1);
        verify(userRepository).save(user);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void deleteThrowsUserNotFoundExceptionWithoutPersistingOrDeleting() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class, () -> userService.delete(1));

        assertEquals("User with id 1 was not found", exception.getMessage());
        verify(userRepository).findById(1);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void deleteKeepsAlreadyInactiveUserInactive() {
        User user = existingUser();
        user.setActive(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.delete(1);

        assertFalse(user.getActive());
        verify(userRepository).findById(1);
        verify(userRepository).save(user);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    private User existingUser() {
        return User.builder()
                .id(1)
                .name("Original name")
                .username("original")
                .passwordHash("existing-hash")
                .active(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private UserResponse responseFor(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserCreateRequest createRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Original name");
        request.setUsername("original");
        request.setPassword("plain-password");
        return request;
    }

    private UserUpdateRequest updateRequest(String name, String username) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName(name);
        request.setUsername(username);
        return request;
    }
}
