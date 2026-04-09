<p align="center">
  <img src="arconia-logo.png" alt="Arconia" width="200" />
</p>

<h1 align="center">Arconia CLI</h1>

<p align="center">
  The companion command-line tool for <a href ="https://dev.java">Java</a>, <a href="https://spring.io/projects/spring-boot">Spring Boot</a>, and <a href="https://docs.arconia.io/arconia/latest/index.html">Arconia</a> projects.
</p>

<p align="center">
  <a href="https://github.com/arconia-io/arconia-cli/actions/workflows/commit-stage.yml"><img src="https://github.com/arconia-io/arconia-cli/actions/workflows/commit-stage.yml/badge.svg" alt="Build" /></a>
  <a href="https://slsa.dev/spec/v1.0/levels"><img src="https://slsa.dev/images/gh-badge-level3.svg" alt="SLSA Level 3" /></a>
  <a href="https://sonarcloud.io/summary/new_code?id=arconia-io_arconia-cli"><img src="https://sonarcloud.io/api/project_badges/measure?project=arconia-io_arconia-cli&metric=alert_status" alt="Quality Gate Status" /></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="Apache 2.0 License" /></a>
  <a href="https://bsky.app/profile/arconia.io"><img src="https://img.shields.io/static/v1?label=Bluesky&message=Follow&color=1DA1F2" alt="Follow us on Bluesky" /></a>
</p>

---

## ✨&nbsp; Features

- **🚀&nbsp; Dev mode**. Run your application with features improving your development experience.
- **🔨&nbsp; Build & test**. Compile, test, and package as JVM JARs or GraalVM native executables.
- **📦&nbsp; Container images**. Build images using Cloud Native Buildpacks or Dockerfiles with Podman/Docker.
- **⬆️&nbsp; Automated upgrades**. Upgrade Spring Boot, Spring AI, Arconia, Gradle, and Maven with a single command.
- **🔄&nbsp; OpenRewrite recipes**. Run any OpenRewrite recipe to migrate or refactor your codebase.
- **📁&nbsp; Project templates**. Create new projects from templates distributed as OCI artifacts.
- **🧠&nbsp; Agent skills**. Install and manage AI agent skills for your applications.

## ⚡&nbsp; Quick Start

**Install (macOS / Linux)**

```shell
brew install arconia-io/tap/arconia-cli
```

**Install (Windows)**

```shell
scoop bucket add arconia https://github.com/arconia-io/scoop-bucket.git
scoop install arconia-cli
```

**Verify and explore**

```shell
arconia version
arconia help
```

**Try it out**

```shell
arconia dev                                     # Run in dev mode with Dev Services
arconia build                                   # Build the application
arconia image build buildpacks                  # Build a container image
arconia update spring-boot --to-version=4.0     # Upgrade Spring Boot to 4.0
```

See the [installation guide](https://arconia.io/arconia-cli/latest/installation.html) for more options, including manual installation and binary downloads.

> [!NOTE]
> The Arconia CLI is currently in active development. We're working hard to improve it and appreciate your patience as we refine the tool. Feel free to try it out and share your feedback!

## 📙&nbsp; Documentation

The [Arconia CLI documentation](https://arconia.io) covers all available commands, options, and workflows in detail.

## 🤝&nbsp; Contributing

Contributions are welcome! Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

## 🛡️&nbsp; Security

The security process for reporting vulnerabilities is described in [SECURITY.md](SECURITY.md).

## 🖊️&nbsp; License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for more information.
