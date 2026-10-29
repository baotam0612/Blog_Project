package com.backlog.backend.model.response;

import jakarta.validation.constraints.NotNull;

public class ChangeStatusRequest {

    @NotNull(message = "Status không được để trống")
    // không cần check value vì thay đổi theo checkbox FE
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
