package com.backlog.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserDTO {
    private String username;
    private String email;
    private String displayName;
    private String password;
    private List<String> roles;


}
