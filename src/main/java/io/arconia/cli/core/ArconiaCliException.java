package io.arconia.cli.core;

import org.springframework.util.Assert;

public class ArconiaCliException extends RuntimeException {

    private final ArconiaCliTerminal terminal;

    public ArconiaCliException(ArconiaCliTerminal terminal, String message) {
        super(message);
        Assert.notNull(terminal, "terminal cannot be null");
        this.terminal = terminal;
    }

    public ArconiaCliException(ArconiaCliTerminal terminal, String message, Exception ex) {
        super(message, ex);
        Assert.notNull(terminal, "terminal cannot be null");
        this.terminal = terminal;
    }

    public ArconiaCliTerminal getTerminal() {
        return terminal;
    }
  
}
