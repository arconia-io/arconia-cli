package io.arconia.cli.core;

import org.jline.terminal.Terminal;
import org.springframework.stereotype.Component;

@Component
public class ArconiaTerminal {

    private final Terminal terminal;

    public ArconiaTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public void println(String... content) {
        for (String text : content) {
            terminal.writer().println(text);
        }
        terminal.flush();
    }

}
