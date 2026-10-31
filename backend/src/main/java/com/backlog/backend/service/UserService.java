package com.backlog.backend.service;

import com.backlog.backend.model.dto.UserDTO;
import com.backlog.backend.model.request.IntrospectRequest;
import com.backlog.backend.model.request.UserRequest;
import com.backlog.backend.model.response.ChangeStatusRequest;

import java.util.List;


public interface UserService {
    List<UserRequest> getAllUser();
    UserDTO getCurrentUser(String userName);
    UserDTO getUserById(Long id);
    UserDTO createUser(UserRequest userRequest);
    UserDTO updateStatusUser(Long id, ChangeStatusRequest changeStatusRequest);
    UserDTO updateUser(Long id, UserRequest userRequest);
    void deleteUser(Long id);
}
