package com.administrapochi.services;

import com.administrapochi.dtos.users.UserCreateRequest;
import com.administrapochi.dtos.users.UserResponse;
import com.administrapochi.dtos.users.UserUpdateRequest;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.exceptions.UsernameAlreadyExistsException;
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
        return userMapper.toResponse(findUser(id));
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse update(Integer id, UserUpdateRequest request) {
        User user = findUser(id);

        if (request.getUsername() != null
                && !request.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void delete(Integer id) {
        User user = findUser(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
