package com.example.meshlab.inventory.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownItemException extends RuntimeException {

    public UnknownItemException(int itemId) {
        super("unknown item: " + itemId);
    }
}
