package io.arconia.cli.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.annotation.PreDestroy;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Help.Ansi;

@Component
public class ArconiaCliTerminal {

    private final Terminal terminal;

    public ArconiaCliTerminal() throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .provider("ffm")
                .dumb(true)
                .build();
    }

    /** 
     * Returns the JLine terminal's {@link PrintWriter} (auto-flush).
     **/
    public PrintWriter writer() {
        return terminal.writer();
    }

    /** 
     * Returns the current terminal column width as reported by JLine.
     **/
    public int width() {
        return terminal.getWidth();
    }

    // -------------------------------------------------------------------------
    // Output helpers (used by runner / command classes)
    // -------------------------------------------------------------------------

    public void info(String... messages) {
        for (var message : messages) {
            terminal.writer().println(message);
        }
        terminal.writer().flush();
    }

    public void warn(String message) {
        terminal.writer().println(Ansi.AUTO.string("@|yellow 🔥 %s|@".formatted(message)));
        terminal.writer().flush();
    }

    public void error(String message) {
        terminal.writer().println(Ansi.AUTO.string("@|red ❗ %s|@".formatted(message)));
        terminal.writer().flush();
    }

    public void debug(boolean debugEnabled, String message) {
        if (debugEnabled) {
            terminal.writer().println(Ansi.AUTO.string("@|faint 🔍 %s|@".formatted(message)));
            terminal.writer().flush();
        }
    }

    public void verbose(boolean verboseEnabled, String... messages) {
        if (verboseEnabled) {
            info(messages);
        }
    }

    public void success(String message) {
        info("✅ %s".formatted(message));
    }

    public void failure(String message) {
        info("❌ %s".formatted(message));
    }

    public void newLine() {
        terminal.writer().println();
        terminal.writer().flush();
    }

    public void handleException(boolean stacktraceEnabled, String message, Exception exception) {
        if (stacktraceEnabled) {
            var sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw, true));
            error(sw.toString());
        }
        error(message);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @PreDestroy
    public void close() throws IOException {
        terminal.close();
    }

}
