package com.varscon.sendcorp.SendCorp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException{
    final String message;
    public BadRequestException(String message) {
        super(message);
        this.message = message;
    }

    public BadRequestException(String message, Exception exception) {
        super(message, exception);
        this.message = message;
    }
}
