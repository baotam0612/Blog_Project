package com.backlog.backend.model.response;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeStatusRequest {
    @NotNull(message = "Status không được để trống")
    private Integer is_active;
}
