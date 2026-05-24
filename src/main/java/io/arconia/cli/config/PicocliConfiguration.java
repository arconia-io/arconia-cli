package io.arconia.cli.config;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import picocli.CommandLine;

import io.arconia.cli.commands.ArconiaCommand;
import io.arconia.cli.core.CliExceptionHandler;
import io.arconia.cli.terminal.TerminalProvider;

@Configuration(proxyBeanMethods = false)
public final class PicocliConfiguration {

    @Bean
    CommandLine commandLine() {
        Terminal terminal = TerminalProvider.getTerminal();
        CommandLine cmd = new CommandLine(new ArconiaCommand());
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
