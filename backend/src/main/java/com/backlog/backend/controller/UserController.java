package com.backlog.backend.controller;


import com.backlog.backend.Response.PayloadResponse;
import com.backlog.backend.model.dto.UserDTO;
import com.backlog.backend.model.request.IntrospectRequest;
import com.backlog.backend.model.request.UserRequest;
import com.backlog.backend.model.response.ChangeStatusRequest;
import com.backlog.backend.model.response.UserResponse;
import com.backlog.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
     final UserService userService;
    private final ModelMapper modelMapper;

    //    public UserController(UserService userService, ModelMapper modelMapper){
//        this.userService = userService;
//        this.modelMapper = modelMapper;
//    }
        private UserResponse toResponse(UserDTO user) {
            return UserResponse.builder()
                    .userName(user.getUsername())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .build();
        }

    // Get All Users
    @GetMapping("/users/me")
    public PayloadResponse getCurrentUser(Authentication authentication) {
        UserDTO user = userService.getCurrentUser(authentication.getName());
        return new PayloadResponse(HttpStatus.OK, "Get Current User Successfully",
                toResponse(user));
    }


    // Get User By Id
    @GetMapping("/users/{id}")
    public PayloadResponse getUserById(@PathVariable Long id){
            UserDTO userDTO = userService.getUserById(id);
            UserRequest res = modelMapper.map(userDTO, UserRequest.class);
            return new PayloadResponse(HttpStatus.OK, "Find User By Id Successfully", res);
    }



    // Get All Users
    @GetMapping("/users")
    public PayloadResponse getAllUsers(){
        List<UserRequest> listUser = userService.getAllUser();

        return new PayloadResponse(HttpStatus.OK, "Get All User sucessfully", listUser);
    }



    // Create User
    @PostMapping("/users")
    public PayloadResponse createUser(@Valid @RequestBody UserRequest userRequest){
        UserDTO res = userService.createUser(userRequest);
        return new PayloadResponse(HttpStatus.CREATED,"Create Successfully", res);

    }

    // Update User Status
    @PatchMapping("/users/{id}/change-active")
    public PayloadResponse updateStatusUser(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest changeStatusRequest){
        UserDTO res = userService.updateStatusUser(id, changeStatusRequest);
        return new PayloadResponse(HttpStatus.OK, "Update Status Sucessfully", res);

    }

    // Update User
    @PutMapping("/users/{id}")
    public PayloadResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest){
        UserDTO res = userService.updateUser(id, userRequest);
        return new PayloadResponse(HttpStatus.OK, "Update Sucessfully", res);
    }

    // Delete User
    @DeleteMapping("/users/{id}")
    public PayloadResponse deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return new PayloadResponse(HttpStatus.OK,"Delete Sucessfully");
    }

}
