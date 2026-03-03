package io.arconia.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import io.arconia.cli.aot.ArconiaCliRuntimeHints;
import picocli.CommandLine;

@SpringBootApplication
@ImportRuntimeHints(ArconiaCliRuntimeHints.class)
public class ArconiaCli implements CommandLineRunner, ExitCodeGenerator {

    private final CommandLine commandLine;
    private int exitCode;

    public ArconiaCli(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    public void run(String... args) {
        exitCode = commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ArconiaCli.class, args)));
    }

}
