package com.example.meshlab.inventory.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ChaosException extends RuntimeException {

    public ChaosException() {
        super("injected failure for rollout testing");
    }
}
