package com.backlog.backend.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class UserRequest {

    @NotBlank(message = "Không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Không được để trống")
    @Email(message = "Email phải nhập đúng định dạng email")
    @Size(max = 30)
    private String email;

    @NotBlank(message = "Không được để trống")
    @Size(min = 8, max = 70, message = "Password nhập tối thiểu 8 kí tự và tối đa 70 kí tự")
    private String password;

    @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
    private String displayName;

    private List<String> roles;



}