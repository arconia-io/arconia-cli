package io.arconia.cli.config;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.cli.commands.ArconiaCommand;
import io.arconia.cli.core.CliExceptionHandler;
import picocli.CommandLine;

@Configuration(proxyBeanMethods = false)
public final class PicocliConfiguration {

    @Bean
    Terminal terminal() throws IOException {
        return TerminalBuilder.builder()
                    .system(true)
                    .provider("ffm")
                    .dumb(true)
                    .build();
    }

    @Bean
    CommandLine commandLine(
            Terminal terminal,
            ArconiaCommand topCommand,
            PicocliSpringFactory factory
    ) {
        CommandLine cmd = new CommandLine(topCommand, factory);
        cmd.setExecutionExceptionHandler(new CliExceptionHandler());
        cmd.setOut(terminal.writer());
        cmd.setErr(terminal.writer());
        cmd.setUsageHelpAutoWidth(true);

        int terminalWidth = terminal.getWidth();
        if (terminalWidth > 0) {
            cmd.setUsageHelpWidth(terminalWidth);
        }

        return cmd;
    }

}
