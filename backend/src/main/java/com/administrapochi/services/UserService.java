package com.administrapochi.services;

import com.administrapochi.models.dtos.users.UserCreateRequest;
import com.administrapochi.models.dtos.users.UserResponse;
import com.administrapochi.models.dtos.users.UserUpdateRequest;

import java.util.List;

public interface UserService {

    List<UserResponse> findAll();

    UserResponse findById(Integer id);

    UserResponse create(UserCreateRequest request);

    UserResponse update(Integer id, UserUpdateRequest request);

    void delete(Integer id);
}
