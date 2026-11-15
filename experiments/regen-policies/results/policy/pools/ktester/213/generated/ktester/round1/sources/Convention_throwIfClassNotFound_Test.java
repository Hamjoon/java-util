package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfClassNotFound_Test {

    private static Stream<Arguments> classNotFoundProvider() {
        ClassLoader failingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                // Simulate a loader that always fails with ClassNotFoundException.
                throw new ClassNotFoundException();
            }
        };
        return Stream.of(// Completely unknown class – normal loader.
        Arguments.of("com.nonexistent.Foo", Convention_throwIfClassNotFound_Test.class.getClassLoader(), "Unknown class: com.nonexistent.Foo was not found."), // Whitespace‑only name – passes null/empty check, then lookup fails.
        Arguments.of("   ", Convention_throwIfClassNotFound_Test.class.getClassLoader(), "Unknown class:    was not found."), // Loader that throws ClassNotFoundException for any request.
        Arguments.of("java.lang.String", failingLoader, "Unknown class: java.lang.String was not found."));
    }

    private static Stream<Arguments> successfulProvider() {
        return Stream.of(Arguments.of("java.lang.Integer", Convention_throwIfClassNotFound_Test.class.getClassLoader()), Arguments.of("java.util.ArrayList", Convention.class.getClassLoader()));
    }

    @Test
    void testThrowIfClassNotFound_whenClassIsNotFound_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("non.existent.FooBar", Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("Unknown class");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfClassNotFound_whenClassNameNullOrEmpty_throwsException(String fqName) {
        // The method must reject null or empty class names before any other checks.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(fqName, Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("fully qualified ClassName cannot be null or empty");
    }

    @Test
    void testThrowIfClassNotFound_whenClassLoaderIsNull_throwsException() {
        // Even with a valid class name, a null loader is not allowed.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", null)).withMessageContaining("loader cannot be null");
    }

    @Test
    void testThrowIfClassNotFound_withValidClassName_andNonNullClassLoader_doesNotThrowException() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", Convention_throwIfClassNotFound_Test.class.getClassLoader()));
    }

    @Test
    void testThrowIfClassNotFound_whenLoaderThrowsSecurityException_propagates() {
        ClassLoader securityThrowingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new SecurityException("access denied");
            }
        };
        // The method must not swallow SecurityException – it should propagate.
        assertThatExceptionOfType(SecurityException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", securityThrowingLoader)).withMessageContaining("access denied");
    }

    @Test
    void testThrowIfClassNotFound_whenClassNameHasSurroundingWhitespace_throwsException() {
        String nameWithSpaces = " java.lang.String ";
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(nameWithSpaces, Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("Unknown class:  java.lang.String  was not found.");
    }
}
