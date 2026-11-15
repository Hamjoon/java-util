package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfClassNotFound_Test {

    @Test
    void testThrowIfClassNotFound_whenClassIsNotFound_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("foo.bar.NonExistingClass", Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("Unknown class");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfClassNotFound_whenClassNameNullOrEmpty(String fqName) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(fqName, Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("fully qualified ClassName cannot be null or empty");
    }

    @Test
    void testThrowIfClassNotFound_whenClassLoaderIsNull_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", null)).withMessageContaining("loader cannot be null");
    }

    @Test
    void testThrowIfClassNotFound_withValidClassName_andNonNullClassLoader_doesNotThrowException() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", Convention_throwIfClassNotFound_Test.class.getClassLoader()));
    }

    @Test
    void testThrowIfClassNotFound_mockedClassNotFound_throwsException() {
        // Mock the static utility to force a null return even for a valid class name
        try (MockedStatic<ClassUtilities> mocked = mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.forName(eq("java.lang.String"), any(ClassLoader.class))).thenReturn(null);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("Unknown class");
        }
    }

    /**
     * Successful execution – the class exists and the loader is non‑null.
     * Uses a few well‑known JDK classes to demonstrate that the method returns normally.
     */
    @ParameterizedTest
    @ValueSource(strings = { "java.lang.String", "java.util.ArrayList", "java.util.HashMap" })
    void testThrowIfClassNotFound_successfulExecution(String className) {
        // Use the current thread's context class loader as required by the test case description
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assertThatNoException().isThrownBy(() -> Convention.throwIfClassNotFound(className, loader));
    }

    /**
     * The class name argument is {@code null} or empty.
     * The method should fail fast via {@code throwIfNullOrEmpty}.
     */
    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfClassNotFound_nullOrEmptyClassName(String fqName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(fqName, loader)).withMessageContaining("fully qualified ClassName cannot be null or empty");
    }

    /**
     * The supplied {@link ClassLoader} is {@code null}.
     * The method should fail via {@code throwIfNull}.
     */
    @Test
    void testThrowIfClassNotFound_nullLoader() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", null)).withMessageContaining("loader cannot be null");
    }

    /**
     * The class cannot be found – {@code ClassUtilities.forName} returns {@code null}.
     */
    @Test
    void testThrowIfClassNotFound_classNotFound() {
        // Use a class name that is guaranteed not to exist.
        String missingClass = "com.nonexistent.FooBarBaz";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(missingClass, loader)).withMessageContaining("Unknown class").withMessageContaining(missingClass);
    }
}
