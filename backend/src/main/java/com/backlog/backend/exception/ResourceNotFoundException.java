package com.backlog.backend.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceNotFoundException extends RuntimeException{
    private String message;



}
