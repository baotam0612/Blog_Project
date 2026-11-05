package com.backlog.backend.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAuthCacheDTO {
    Long id;
    String username;
    Boolean active;
    List<String> roles;
}
