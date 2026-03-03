package io.arconia.cli.core;

public class ArconiaCliException extends RuntimeException {

    public ArconiaCliException(String message) {
        super(message);
    }

    public ArconiaCliException(String message, Throwable cause) {
        super(message, cause);
    }

}
