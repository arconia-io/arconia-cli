package io.arconia.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@CommandScan
public class ArconiaCli {

	public static void main(String[] args) {
		SpringApplication.run(ArconiaCli.class, args);
	}

}
