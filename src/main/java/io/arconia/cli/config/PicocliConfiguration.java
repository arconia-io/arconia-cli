package io.arconia.cli.config;

import java.io.PrintWriter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.cli.commands.ArconiaCommand;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.core.ArconiaExceptionHandler;
import picocli.CommandLine;

@Configuration(proxyBeanMethods = false)
public final class PicocliConfiguration {

    @Bean
    CommandLine commandLine(
            ArconiaCommand topCommand,
            PicocliSpringFactory factory,
            ArconiaExceptionHandler exceptionHandler,
            ArconiaCliTerminal terminal
    ) {
        CommandLine cmd = new CommandLine(topCommand, factory);
        cmd.setExecutionExceptionHandler(exceptionHandler);
        cmd.setOut(new PrintWriter(terminal.writer(), true));
        cmd.setErr(new PrintWriter(terminal.writer(), true));
        cmd.setUsageHelpAutoWidth(true);

        // In headless / test environments JLine returns 0; skip the explicit
        // width in that case and let usageHelpAutoWidth handle it.
        int terminalWidth = terminal.width();
        if (terminalWidth > 0) {
            cmd.setUsageHelpWidth(terminalWidth);
        }

        return cmd;
    }

}
