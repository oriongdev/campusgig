package com.campusgig.matching.service;

/** Thrown when the LLM replies with something the application cannot use. */
public class AiException extends RuntimeException {

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
