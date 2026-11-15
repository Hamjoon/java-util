package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.AccessControlException;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfClassNotFound_Test {

    @Test
    void testThrowIfClassNotFound_whenClassIsNotFound_throwsException() {
        try (MockedStatic<ClassUtilities> mocked = mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.forName("com.nonexistent.FakeClass", Convention_throwIfClassNotFound_Test.class.getClassLoader())).thenReturn(null);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("com.nonexistent.FakeClass", Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("Unknown class: com.nonexistent.FakeClass was not found.");
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfClassNotFound_whenClassNameNullOrEmpty_throwsException(String fqName) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(fqName, Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("fully qualified ClassName cannot be null or empty");
    }

    @Test
    void testThrowIfClassNotFound_whenClassLoaderIsNull_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", null)).withMessageContaining("loader cannot be null");
    }

    @Test
    void testThrowIfClassNotFound_withValidClassName_andNonNullClassLoader_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", Convention_throwIfClassNotFound_Test.class.getClassLoader()));
    }

    @Test
    void testThrowIfClassNotFound_whenWhitespaceClassName_throwsException() {
        String whitespaceName = "   ";
        try (MockedStatic<ClassUtilities> mocked = mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.forName(whitespaceName, Convention_throwIfClassNotFound_Test.class.getClassLoader())).thenReturn(null);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(whitespaceName, Convention_throwIfClassNotFound_Test.class.getClassLoader())).withMessageContaining("Unknown class:    was not found.");
        }
    }

    @Test
    void testThrowIfClassNotFound_withPrimitiveTypeName_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfClassNotFound("int", Convention_throwIfClassNotFound_Test.class.getClassLoader()));
    }

    @Test
    void testThrowIfClassNotFound_withArrayClassName_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String[]", Convention_throwIfClassNotFound_Test.class.getClassLoader()));
    }

    @Test
    void testThrowIfClassNotFound_whenBothParametersNull_validationOrder() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfClassNotFound(null, null)).withMessageContaining("fully qualified ClassName cannot be null or empty");
    }

    @Test
    void testThrowIfClassNotFound_loaderThrowsSecurityException_propagates() {
        ClassLoader securityThrowingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("java.lang.String".equals(name)) {
                    throw new AccessControlException("security violation");
                }
                return super.loadClass(name);
            }
        };
        assertThatExceptionOfType(AccessControlException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", securityThrowingLoader)).withMessageContaining("security violation");
    }

    @Test
    void testThrowIfClassNotFound_loaderThrowsRuntimeException_propagates() {
        ClassLoader runtimeThrowingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("java.lang.String".equals(name)) {
                    throw new IllegalStateException("runtime failure");
                }
                return super.loadClass(name);
            }
        };
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", runtimeThrowingLoader)).withMessageContaining("runtime failure");
    }

    @Test
    void testThrowIfClassNotFound_loaderThrowsLinkageError_propagates() {
        ClassLoader errorThrowingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("java.lang.String".equals(name)) {
                    throw new NoClassDefFoundError("missing dependency");
                }
                return super.loadClass(name);
            }
        };
        assertThatExceptionOfType(NoClassDefFoundError.class).isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", errorThrowingLoader)).withMessageContaining("missing dependency");
    }
}
