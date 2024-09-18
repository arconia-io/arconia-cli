# Arconia CLI

![Build](https://github.com/arconia-io/arconia-cli/actions/workflows/commit-stage.yml/badge.svg)
[![The SLSA Level 3 badge](https://slsa.dev/images/gh-badge-level3.svg)](https://slsa.dev/spec/v1.0/levels)
[![The Apache 2.0 license badge](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Follow us on Twitter](https://img.shields.io/static/v1?label=Twitter&message=Follow&color=1DA1F2)](https://twitter.com/vitalethomas)

Arconia CLI is a versatile tool designed to enhance the development experience with Spring Boot applications, providing intuitive commands for building, testing, and running projects. It streamlines common tasks, including upgrading Spring Boot applications to the latest version with a single command, and simplifies containerization using both Cloud Native Buildpacks and Dockerfiles.

<img src="arconia-logo.png" alt="The Arconia logo" height="250px" />

> [!NOTE]
> The Arconia CLI is currently in active development. We're working hard to improve it and appreciate your patience as we refine the tool. Feel free to try it out and share your feedback!

## 🚀&nbsp; Getting Started

### Installation

Download the appropriate binary for your operating system from the [latest Arconia CLI release](https://github.com/arconia-io/arconia-cli/releases). Then, follow the instructions below to install it on your system.

#### macOS and Linux

1. Move the downloaded binary to a directory in your system PATH:

```shell
mv ~/Downloads/arconia-[version]-[os]/bin/ /usr/local/bin/
```

2. Make the binary executable:

```shell
chmod +x /usr/local/bin/arconia
```

#### Windows

Run the following commands in PowerShell with administrator privileges:

```shell
# Create a directory for Arconia CLI
New-Item -ItemType Directory -Path "$env:ProgramFiles\arconia-cli" -Force

# Move the executable to the new directory
Move-Item -Path "$env:USERPROFILE\Downloads\arconia-[version]-[os]\bin\arconia.exe" -Destination "$env:ProgramFiles\arconia-cli\arconia.exe"

# Add the new directory to the system PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
$newPath = "$currentPath;$env:ProgramFiles\arconia-cli"
[Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")

# Refresh the current PowerShell session's PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
```

### Usage

To get started with Arconia CLI, you can view the available commands and options by running:

```shell
arconia help
```

This will display a list of commands and their descriptions, helping you navigate the CLI's functionality.

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

## 🔗&nbsp; Provenance

### PGP

All Arconia CLI release artifacts are signed with PGP. Follow these steps to verify the authenticity of the downloads.

1. Download the Arconia [public key](http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x36DD645BC7818CF5C884DE8F2E64062497575B2D) and save it as `arconia.asc`.
2. Verify the key fingerprint matches the following:

```shell
$ gpg --show-keys arconia.asc
pub   ed25519 2024-01-16 [SC] [expires: 2026-01-15]
      36DD645BC7818CF5C884DE8F2E64062497575B2D
uid                      Thomas Vitale (MavenCentral) <oss@************.io>
sub   cv25519 2024-01-16 [E] [expires: 2026-01-15]
```

3. Import the public key into your GPG keyring:

```shell
$ gpg --import arconia.asc
```

4. Download the Arconia CLI binary archive and its corresponding signature file from the desired [release](https://github.com/arconia-io/arconia-cli/releases)
For example:

* `arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip`
* `arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip.asc`.

5. Verify the signature against the downloaded binary:

```shell
$ gpg --verify arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip.asc arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip
gpg: Signature made Mar 17 Set 23:29:59 2024 CEST
gpg:                using EDDSA key 2E64062497575B2D
gpg: Good signature from "Thomas Vitale (MavenCentral) <oss@************.io>" [ultimate]
```

A successful verification will display "Good signature" in the output, confirming the authenticity and integrity of the downloaded binary.

### SLSA

Every Arconia CLI release includes [SLSA](https://slsa.dev) provenance attestations for all release artifacts. Follow these steps to verify the provenance.

1. Install the [slsa-verifier](https://github.com/slsa-framework/slsa-verifier) tool
2. Download the SLSA provenance attestation file from the desired [release](https://github.com/arconia-io/arconia-cli/releases).
Example: `arconia-cli-0.0.1-SNAPSHOT.intoto.jsonl`.
3. Download the corresponding Arconia CLI binary archive from the same release.
Example: `arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip`.
4. Use the slsa-verifier to verify the SLSA provenance attestation against the binary:

```shell
$ slsa-verifier verify-artifact arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip \
       --provenance-path arconia-cli-0.0.1-SNAPSHOT.intoto.jsonl \
       --source-uri github.com/arconia-io/arconia-cli
```

A successful verification will output a message similar to:

```
Verified signature against tlog entry index 131481633 at URL: https://rekor.sigstore.dev/api/v1/log/entries/108e9186e8c5677a1631335a14958734e5e0a00b4105b318339d4571b91a1ab8a8b2a90b1704d6d0
Verified build using builder "https://github.com/slsa-framework/slsa-github-generator/.github/workflows/generator_generic_slsa3.yml@refs/tags/v2.0.0" at commit 10d734affc77f0f4d0f1087fe66bd7eeb3a61f8a
Verifying artifact arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip: PASSED

PASSED: SLSA verification passed
```

This process ensures the authenticity and integrity of the Arconia CLI release artifacts.

### SBOM

Every Arconia CLI release includes a comprehensive [CycloneDX](https://cyclonedx.org/) Software Bill of Materials (SBOM) for all release artifacts. The SBOM provides a detailed inventory of components, libraries, and dependencies used in the Arconia CLI, enhancing transparency, security, and compliance efforts.

To access and review the SBOM for a specific release:

1. Visit the [Arconia CLI releases page](https://github.com/arconia-io/arconia-cli/releases) on GitHub.
2. Download the binary archive for your desired release and platform.
Example: `arconia-cli-0.0.1-SNAPSHOT-linux-amd64.zip`.
3. Extract the contents of the archive to a local directory.
4. Locate the file named `sbom.cdx.json` within the extracted directory. This JSON file contains the comprehensive SBOM for the release.

You can use various SBOM analysis tools to examine the `sbom.cdx.json` file and gain valuable insights into the components and dependencies of the Arconia CLI. Some recommended tools include:

- [OWASP Dependency-Track](https://dependencytrack.org/): An intelligent component analysis platform that allows you to identify and reduce risk in your software supply chain.
- [Trivy](https://github.com/aquasecurity/trivy): A comprehensive and versatile security scanner for containers and other artifacts.

By reviewing the SBOM, you can better understand the composition of the Arconia CLI, identify potential vulnerabilities, and ensure compliance with licensing requirements.

## 💻&nbsp; Development

### Prerequisites

* Java 23 (GraalVM)

### Building the CLI

To package the Arconia CLI as a native executable, run:

```shell
./gradlew nativeCompile
```

This command compiles the project and generates a standalone native executable. The resulting file will be located at `build/native/nativeCompile/arconia`.

### Installing the CLI

You can run the CLI directly from its build location or install it system-wide for easier access. To install it globally on macOS or Linux:

```shell
sudo cp build/native/nativeCompile/arconia /usr/local/bin/arconia
```

This command copies the executable to a directory typically included in the system PATH, making it accessible from anywhere in the terminal.

### Running the CLI

If you've installed the CLI globally, you can run it by simply typing:

```shell
arconia help
```

If you haven't installed it globally, you'll need to specify the full path to the executable:

```shell
./build/native/nativeCompile/arconia help
```

Replace `help` with any other valid command or option to use different features of the CLI.

## 🛡️&nbsp; Security

The security process for reporting vulnerabilities is described in [SECURITY.md](SECURITY.md).

## 🖊️&nbsp; License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for more information.

## 🙏&nbsp; Acknowledgments

This project is built upon [Spring Shell](https://docs.spring.io/spring-shell/reference/index.html), a powerful framework for creating CLI applications. We also draw inspiration from the exceptional user experience provided by the [Quarkus CLI](https://quarkus.io/guides/cli-tooling), which has influenced our design.
