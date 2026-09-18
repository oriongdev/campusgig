package com.campusgig.matching.service;

/** Thrown when the AI agents are asked for something but no LLM is configured. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }
}
