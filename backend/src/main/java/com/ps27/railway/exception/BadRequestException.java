package com.ps27.railway.exception;

/** Thrown when a request is semantically invalid (bad dates, bad references, etc.). Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
