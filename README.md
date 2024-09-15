# Arconia CLI

The Arconia CLI is a tool aimed at improving the developer experience when working
with Spring Boot applications.

<img src="arconia-logo.png" alt="The Arconia logo" height="250px" />

> [!IMPORTANT]
> The Arconia CLI is under active development. Use at your own risk!

## 🚀&nbsp; Getting Started

### Prerequisites

* Java 22 (GraalVM)

### Build the CLI

Package the Arconia CLI as a native executable:

```shell
./gradlew nativeCompile
```

The executable is located in `build/native/nativeCompile/arconia-cli`.
You can run it from there or add it as a new executable to your OS Path.
For example, on macOS and Linux:

```shell
sudo cp build/native/nativeCompile/arconia-cli /usr/local/bin/arconia
```

### Run the CLI

If you defined the Arconia CLI as an executable in your OS Path, you can run it as follows:

```shell
arconia help
```

Otherwise, you can point to the executable file (`build/native/nativeCompile/arconia-cli`).

## 📙&nbsp; Reference

```
AVAILABLE COMMANDS

Built-In Commands
       help: Display help about available commands
       version: Show version info

Development
       build: Build the current project.
       test: Run tests for the current project.
       run: Run the application.

Image
       image build buildpacks: Build a container image using Buildpacks.
       image build dockerfile: Build a container image using Dockerfile.

Migration
       update: Update project to new Spring Boot version.
```

## 🛡️&nbsp; Security

The security process for reporting vulnerabilities is described in [SECURITY.md](SECURITY.md).

## 🖊️&nbsp; License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for more information.

## 🙏&nbsp; Acknowledgments

This project relies on [Spring Shell](https://docs.spring.io/spring-shell/reference/index.html) for building CLI applications and draws inspiration from the superior experience offered by the [Quarkus CLI](https://quarkus.io/guides/cli-tooling).
