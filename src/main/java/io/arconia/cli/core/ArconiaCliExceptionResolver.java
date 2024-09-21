package io.arconia.cli.core;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.stereotype.Component;

@Component
public class ArconiaCliExceptionResolver implements CommandExceptionResolver {

    @Override
    public CommandHandlingResult resolve(Exception ex) {
        if (ex instanceof ArconiaCliException arconiaCliException) {
            arconiaCliException.getTerminal().handleException(ex.getMessage(), ex);
            return CommandHandlingResult.empty();
        }

        var attributedStringBuilder = new AttributedStringBuilder();
        attributedStringBuilder.append(new AttributedString(ex.getMessage(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)));
        attributedStringBuilder.append("\n");
        var attributedString = attributedStringBuilder.toAttributedString().toAnsi();
        return CommandHandlingResult.of(attributedString);
    }

}
