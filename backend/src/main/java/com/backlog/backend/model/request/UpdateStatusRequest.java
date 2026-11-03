package com.backlog.backend.model.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {
    @NotBlank(message = "Status không được để trống")
    @Pattern(
            regexp = "DRAFT|PUBLISHED",
            message = "Status chỉ được là DRAFT hoặc PUBLISHED"
    )
    private String status;
}
