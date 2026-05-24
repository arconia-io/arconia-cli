package io.arconia.cli.terminal;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class TerminalProvider {

    private static final Terminal terminal;

    static {
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .provider("ffm")
                    .dumb(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Terminal getTerminal() {
        return terminal;
    }

}
