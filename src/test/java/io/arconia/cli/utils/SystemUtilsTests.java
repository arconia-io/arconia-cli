package io.arconia.cli.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SystemUtils}.
 */
class SystemUtilsTests {

    @Test
    void isWindowsReturnsTrueOnWindows() {
        var original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 10");
            assertThat(SystemUtils.isWindows()).isTrue();
        } finally {
            System.setProperty("os.name", original);
        }
    }

    @Test
    void isWindowsReturnsTrueOnWindowsServerVariant() {
        var original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows Server 2022");
            assertThat(SystemUtils.isWindows()).isTrue();
        } finally {
            System.setProperty("os.name", original);
        }
    }

    @Test
    void isWindowsReturnsFalseOnMac() {
        var original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertThat(SystemUtils.isWindows()).isFalse();
        } finally {
            System.setProperty("os.name", original);
        }
    }

    @Test
    void isWindowsReturnsFalseOnLinux() {
        var original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            assertThat(SystemUtils.isWindows()).isFalse();
        } finally {
            System.setProperty("os.name", original);
        }
    }

}
