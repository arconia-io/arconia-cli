package io.arconia.cli.utils;

public class ProcessExecutionException extends RuntimeException {

    public ProcessExecutionException(String message, String[] command) {
        super("%s: %s".formatted(message, String.join(" ", command)));
    }

    public ProcessExecutionException(String message, String[] command, Exception ex) {
        super("%s: %s".formatted(message, String.join(" ", command)), ex);
    }
  
}
