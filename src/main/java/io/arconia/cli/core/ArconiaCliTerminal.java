package io.arconia.cli.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.command.CommandContext;
import org.springframework.util.Assert;

public class ArconiaCliTerminal {

    private final CommandContext commandContext;
    private final Terminal terminal;

    public ArconiaCliTerminal(CommandContext commandContext) {
        Assert.notNull(commandContext, "commandContext cannot be null");
        Assert.notNull(commandContext.getTerminal(), "commandContext.getTerminal() cannot be null");

        this.commandContext = commandContext;
        this.terminal = commandContext.getTerminal();

        debug("Arguments: %s".formatted(String.join(" ", commandContext.getRawArgs())));
    }

    public boolean isDebug() {
        return commandContext.getOptionValue("debug") instanceof Boolean debug && debug;
    }

    public boolean isVerbose() {
        return commandContext.getOptionValue("verbose") instanceof Boolean verbose && verbose || isDebug();
    }

    public boolean isStacktrace() {
        return commandContext.getOptionValue("stacktrace") instanceof Boolean stacktrace && stacktrace;
    }

    public void info(String... messages) {
        for (var message : messages) {
            terminal.writer().println(message);
        }
        terminal.flush();
    }

    public void warn(String message) {
        var warnMessage = buildMessageWithStyle("🔥 %s".formatted(message), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        write(warnMessage);
    }

    public void error(String message) {
        var errorMessage = buildMessageWithStyle("❗ %s".formatted(message), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        write(errorMessage);
    }

    public void debug(String message) {
        if (isDebug()) {
            var debugMessage = buildMessageWithStyle("🔍 %s".formatted(message), AttributedStyle.DEFAULT.faint());
            write(debugMessage);
        }
    }

    public void verbose(String... message) {
        if (isVerbose()) {
            info(message);
        }
    }

    public void success(String message) {
        info("✅ %s".formatted(message));
    }

    public void failure(String message) {
        info("❌ %s".formatted(message));
    }

    public void newLine() {
        terminal.writer().println("\n");
        terminal.flush();
    }

    public void handleException(String message, Exception exception) {
        if (isStacktrace()) {
            var stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter, true));
            error(stringWriter.toString());
        }

        error(message);
    }

    private String buildMessageWithStyle(String message, AttributedStyle style) {
        var messageBuilder = new AttributedStringBuilder();
        messageBuilder.append(new AttributedString(message, style));
        return messageBuilder.toAttributedString().toAnsi();
    }

    private void write(String message) {
        terminal.writer().println(new String(message));
        terminal.flush();
    }

}
