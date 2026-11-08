package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import static org.mockito.ArgumentMatchers.any;

class ClassUtilities_loadResourceAsBytes_Test {

    /**
     * InputStream that throws an {@link IOException} on the first {@code read()} call.
     */
    private static class IOExceptionOnReadInputStream extends InputStream {

        private boolean firstRead = true;

        @Override
        public int read() throws IOException {
            if (firstRead) {
                firstRead = false;
                throw new IOException("simulated read error");
            }
            return -1;
        }
    }

    /**
     * InputStream that throws an {@link IOException} when {@code close()} is called.
     */
    private static class IOExceptionOnCloseInputStream extends InputStream {

        private final byte[] data;

        private int pos = 0;

        IOExceptionOnCloseInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            if (pos >= data.length) {
                return -1;
            }
            return data[pos++] & 0xFF;
        }

        @Override
        public void close() throws IOException {
            throw new IOException("simulated close error");
        }
    }

    /**
     * Returns a simple {@link ClassLoader} that supplies the given {@link InputStream}
     * for any resource name.
     */
    private static ClassLoader classLoaderReturning(InputStream stream) {
        return new ClassLoader() {

            @Override
            public InputStream getResourceAsStream(String name) {
                return stream;
            }
        };
    }

    /**
     * Returns a {@link ClassLoader} that throws the supplied {@link RuntimeException}
     * (or {@link SecurityException}) when {@code getResourceAsStream} is invoked.
     */
    private static ClassLoader classLoaderThrowing(RuntimeException ex) {
        return new ClassLoader() {

            @Override
            public InputStream getResourceAsStream(String name) {
                throw ex;
            }
        };
    }

    /**
     * Returns a {@link ClassLoader} that returns {@code null} for any resource name,
     * simulating a missing resource.
     */
    private static ClassLoader classLoaderReturningNull() {
        return new ClassLoader() {

            @Override
            public InputStream getResourceAsStream(String name) {
                return null;
            }
        };
    }

    /**
     * Verify that a known class‑file resource can be loaded as a non‑empty byte array.
     */
    @Test
    @DisplayName("loadResourceAsBytes – existing resource returns bytes")
    void testLoadExistingResource() {
        // The compiled class file for java.lang.String is always present on the classpath.
        String resourceName = "java/lang/String.class";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(bytes, "Returned byte array should not be null");
        assertTrue(bytes.length > 0, "Returned byte array should contain data");
    }

    /**
     * Verify that passing {@code null} as the resource name results in a {@link NullPointerException}.
     */
    @Test
    @DisplayName("loadResourceAsBytes – null argument throws NPE")
    void testLoadResourceNameIsNull() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsBytes(null), "Expected NullPointerException when resourceName is null");
    }

    /**
     * Verify that a non‑existent resource name triggers an {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("loadResourceAsBytes – unknown resource throws IllegalArgumentException")
    void testLoadNonExistingResource() {
        String bogusName = "this/does/not/exist/foobar.txt";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsBytes(bogusName), "Expected IllegalArgumentException for missing resource");
        assertTrue(ex.getMessage().contains(bogusName), "Exception message should contain the missing resource name");
    }

    @Test
    @DisplayName("NullResourceName – null argument throws NullPointerException")
    void testNullResourceName() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsBytes(null));
        assertTrue(npe.getMessage().contains("resourceName cannot be null"));
    }

    @Test
    @DisplayName("ResourceNotFound – unknown resource throws IllegalArgumentException")
    void testResourceNotFound() {
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturningNull());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsBytes("nonexistent.txt"));
            assertTrue(ex.getMessage().contains("Resource not found"));
        }
    }

    @Test
    @DisplayName("IOExceptionDuringRead – read error is wrapped in UncheckedIOException")
    void testIOExceptionDuringRead() {
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new IOExceptionOnReadInputStream()));
            UncheckedIOException uio = assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsBytes("ioerror.txt"));
            assertTrue(uio.getCause() instanceof IOException);
            assertEquals("simulated read error", uio.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("SuccessfulRead – known resource returns expected bytes")
    void testSuccessfulRead() {
        byte[] expected = "sample\n".getBytes(StandardCharsets.UTF_8);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new ByteArrayInputStream(expected)));
            byte[] actual = ClassUtilities.loadResourceAsBytes("sample.txt");
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("EmptyResourceNameString – empty string treated as missing resource")
    void testEmptyResourceNameString() {
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturningNull());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsBytes(""));
            assertTrue(ex.getMessage().contains("Resource not found"));
        }
    }

    @Test
    @DisplayName("EmptyFile – existing but zero‑byte resource returns empty array")
    void testEmptyFile() {
        byte[] empty = new byte[0];
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new ByteArrayInputStream(empty)));
            byte[] actual = ClassUtilities.loadResourceAsBytes("empty.txt");
            assertNotNull(actual);
            assertEquals(0, actual.length);
        }
    }

    @Test
    @DisplayName("LeadingSlashResource – leading '/' is ignored and bytes are returned")
    void testLeadingSlashResource() {
        byte[] expected = "sample\n".getBytes(StandardCharsets.UTF_8);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new ByteArrayInputStream(expected)));
            byte[] actual = ClassUtilities.loadResourceAsBytes("/sample.txt");
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("BinaryFileRead – binary resource returns exact byte sequence")
    void testBinaryFileRead() {
        byte[] expected = new byte[] { 0, (byte) 255, 127, 64 };
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new ByteArrayInputStream(expected)));
            byte[] actual = ClassUtilities.loadResourceAsBytes("binary.bin");
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("UnicodeResourceName – resource name with µ returns UTF‑8 bytes")
    void testUnicodeResourceName() {
        // 'µ' in UTF‑8
        byte[] expected = new byte[] { (byte) 0xC3, (byte) 0xB5 };
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new ByteArrayInputStream(expected)));
            byte[] actual = ClassUtilities.loadResourceAsBytes("unicode-µ.txt");
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("ResourceNameWithSpaces – spaces in name are handled correctly")
    void testResourceNameWithSpaces() {
        byte[] expected = "space name\n".getBytes(StandardCharsets.UTF_8);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new ByteArrayInputStream(expected)));
            byte[] actual = ClassUtilities.loadResourceAsBytes("space name.txt");
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("SecurityExceptionDuringLoad – SecurityException propagates")
    void testSecurityExceptionDuringLoad() {
        SecurityException secEx = new SecurityException("access denied");
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderThrowing(secEx));
            SecurityException thrown = assertThrows(SecurityException.class, () -> ClassUtilities.loadResourceAsBytes("secure.txt"));
            assertEquals("access denied", thrown.getMessage());
        }
    }

    @Test
    @DisplayName("IOExceptionOnClose – IOException during close is wrapped")
    void testIOExceptionOnClose() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(classLoaderReturning(new IOExceptionOnCloseInputStream(data)));
            UncheckedIOException uio = assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsBytes("closeerror.txt"));
            assertTrue(uio.getCause() instanceof IOException);
            assertEquals("simulated close error", uio.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("NullClassLoader – getClassLoader returns null leading to NPE")
    void testNullClassLoader() {
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
            mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(// simulate null class loader
            null);
            NullPointerException npe = assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsBytes("any.txt"));
            // The exact message is not defined by the method; we only assert that NPE occurs.
            assertNotNull(npe);
        }
    }
}
