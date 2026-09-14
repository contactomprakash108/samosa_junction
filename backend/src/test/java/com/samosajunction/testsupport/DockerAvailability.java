package com.samosajunction.testsupport;

import org.testcontainers.DockerClientFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DockerAvailability {

    private DockerAvailability() {
    }

    public static boolean isAvailable() {
        try {
            if (!Files.exists(Path.of("/var/run/docker.sock"))) {
                return false;
            }
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
