package io.arconia.cli.commands.options;

import picocli.CommandLine.Option;

public class RegistryOptions {

    @Option(names = {"--registry-insecure"}, description = "Call the OCI registry over HTTP instead of HTTPS.")
    boolean registryInsecure;

    @Option(names = {"--registry-skip-tls-verify"}, description = "Allow TLS connections without validating certificates.")
    boolean registrySkipTlsVerify;

    public boolean registryInsecure() {
        return registryInsecure;
    }

    public boolean registrySkipTlsVerify() {
        return registrySkipTlsVerify;
    }

}
