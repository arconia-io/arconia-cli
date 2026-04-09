package io.arconia.cli.image;

import io.arconia.cli.utils.IoUtils;

/**
 * Supported OCI container runtimes for building and managing container images.
 */
public enum OciRuntime {

    DOCKER("docker"),
    PODMAN("podman");

    private final String executableName;

    OciRuntime(String executableName) {
        this.executableName = executableName;
    }

    public String getExecutableName() {
        return executableName;
    }

    /**
     * Detects the available OCI runtime on the system.
     * <p>
     * Checks for Podman first, then falls back to Docker.
     */
    public static OciRuntime detect() {
        if (IoUtils.getExecutable(PODMAN.executableName) != null) {
            return PODMAN;
        }
        if (IoUtils.getExecutable(DOCKER.executableName) != null) {
            return DOCKER;
        }
        throw new IllegalStateException("No supported OCI runtime found. Please install Podman or Docker.");
    }

}
