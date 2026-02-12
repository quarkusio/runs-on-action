package io.quarkus.bot.runson;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.regex.Pattern;

class Images {

    static final String UBUNTU_24 = "ubuntu24-full-x64";
    static final String UBUNTU_22 = "ubuntu22-full-x64";

    // Version with new Keycloak images, see https://github.com/quarkusio/quarkus/pull/49283
    private static final ComparableVersion UBUNTU_24_COMPATIBLE = new ComparableVersion("3.29");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

    static String getUbuntuLatest(String branch) {
        if ("main".equals(branch) || !VERSION_PATTERN.matcher(branch).matches()) {
            return UBUNTU_24;
        }

        return new ComparableVersion(branch).compareTo(UBUNTU_24_COMPATIBLE) >= 0 ? UBUNTU_24 : UBUNTU_22;
    }
}
