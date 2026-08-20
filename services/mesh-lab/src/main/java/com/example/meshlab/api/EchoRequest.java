package com.example.meshlab.api;

import jakarta.validation.constraints.NotBlank;

public record EchoRequest(
        @NotBlank(message = "message must not be blank")
        String message
) {
}
