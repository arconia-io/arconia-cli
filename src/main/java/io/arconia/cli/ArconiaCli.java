package io.arconia.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.shell.command.annotation.CommandScan;

import io.arconia.cli.aot.AiRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(AiRuntimeHints.class)
@CommandScan
public class ArconiaCli {

	public static void main(String[] args) {
		SpringApplication.run(ArconiaCli.class, args);
	}

}
