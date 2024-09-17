# Arconia CLI

![Commit Stage Workflow](https://github.com/arconia-io/arconia-cli/actions/workflows/commit-stage.yml/badge.svg)
[![The SLSA Level 3 badge](https://slsa.dev/images/gh-badge-level3.svg)](https://slsa.dev/spec/v1.0/levels)
[![The Apache 2.0 license badge](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Follow us on Twitter](https://img.shields.io/static/v1?label=Twitter&message=Follow&color=1DA1F2)](https://twitter.com/kadrasIO)

Arconia CLI is a powerful tool designed to streamline and enhance the developer experience when working
with Spring Boot applications. 

It offers a comprehensive set of convenient commands to build, test, and run Spring Boot applications
effortlessly. With Arconia CLI, you can upgrade your applications to the latest Spring Boot version
with a single command, saving time and ensuring you're always using the most up-to-date features.

Additionally, it simplifies the process of packaging your application as a container image, supporting
both Cloud Native Buildpacks and Dockerfiles. Whether you're a seasoned Spring Boot developer
or just getting started, Arconia CLI aims to boost your productivity and make your development
workflow smoother and more efficient.

<img src="arconia-logo.png" alt="The Arconia logo" height="250px" />

> [!IMPORTANT]
> The Arconia CLI is under active development. Use at your own risk!

## 🚀&nbsp; Getting Started

### Installation

[Download](https://github.com/arconia-io/arconia-cli/releases) the binary for your OS from the latest GitHub release, move it next to your other binaries and make it executable.

On macOS and Linux:

```shell
mv ~/Downloads/arconia-[version]-[os]/bin/ /usr/local/bin/
chmod +x /usr/local/bin/arconia
```

On Windows, run the following commands from PowerShell:

```shell
# Create a new directory for arconia-cli
New-Item -ItemType Directory -Path "$env:ProgramFiles\arconia-[version]-[os]" -Force

# Move the arconia.exe to the new directory
Move-Item -Path "$env:USERPROFILE\Downloads\arconia-[version]-[os]\bin\arconia.exe" -Destination "$env:ProgramFiles\arconia-cli\arconia.exe"

# Add the new directory to the system PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
$newPath = "$currentPath;$env:ProgramFiles\arconia-cli"
[Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")

# Refresh the current PowerShell session's PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
```

### Usage

Get the help information for the Arconia CLI.

```shell
arconia help
```

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

## 📙&nbsp; Provenance

### SLSA

Every Arconia CLI release provides SLSA provenance for all the release artifacts.

1. Install the slsa-verifier CLI
2. Download the SLSA provenance attestation file from the release you want to validate.
For example, `arconia-cli-0.0.1-SNAPSHOT.intoto.jsonl`.
3. Download the Arconia CLI binary archive you want to validate.
For example, `arconia-0.0.1-SNAPSHOT-linux-amd64.zip`.
4. Use the slsa-verifier to verify the SLSA provenance attestation against the binary.
For example,

```shell
$ slsa-verifier verify-artifact arconia-0.0.1-SNAPSHOT-linux-amd64.zip \
       --provenance-path arconia-cli-0.0.1-SNAPSHOT.intoto.jsonl \
       --source-uri github.com/arconia-io/arconia-cli
Verified signature against tlog entry index 131479173 at URL: https://rekor.sigstore.dev/api/v1/log/entries/108e9186e8c5677a7a96a19c05e3e336be9a8ce5f56647734ec1b4737ecfd1e5d15519b88f08a706
Verified build using builder "https://github.com/slsa-framework/slsa-github-generator/.github/workflows/generator_generic_slsa3.yml@refs/tags/v2.0.0" at commit eee4635ed75b717f8ceef47f6a2dce3c8e4bfbeb
Verifying artifact arconia-0.0.1-SNAPSHOT-linux-amd64.zip: PASSED
```

## 💻&nbsp; Development

### Prerequisites

* Java 22 (GraalVM)

### Build the CLI

Package the Arconia CLI as a native executable:

```shell
./gradlew nativeCompile
```

The executable is located in `build/native/nativeCompile/arconia`.
You can run it from there or add it as a new executable to your OS Path.
For example, on macOS and Linux:

```shell
sudo cp build/native/nativeCompile/arconia /usr/local/bin/arconia
```

### Run the CLI

If you defined the Arconia CLI as an executable in your OS Path, you can run it as follows:

```shell
arconia help
```

Otherwise, you can point to the executable file (`build/native/nativeCompile/arconia`).

## 🛡️&nbsp; Security

The security process for reporting vulnerabilities is described in [SECURITY.md](SECURITY.md).

## 🖊️&nbsp; License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for more information.

## 🙏&nbsp; Acknowledgments

This project relies on [Spring Shell](https://docs.spring.io/spring-shell/reference/index.html) for building CLI applications and draws inspiration from the superior experience offered by the [Quarkus CLI](https://quarkus.io/guides/cli-tooling).
