package com.cedarsoftware.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

class ClassUtilities_loadResourceAsBytes_Test {

    /**
     * Simple InputStream that returns a predefined byte array.
     */
    private static class ByteArrayInputStreamStub extends InputStream {

        private final byte[] data;

        private int pos = 0;

        private boolean closed = false;

        ByteArrayInputStreamStub(byte[] data) {
            this.data = data != null ? data : new byte[0];
        }

        @Override
        public int read() throws IOException {
            if (closed)
                throw new IOException("stream closed");
            return (pos < data.length) ? (data[pos++] & 0xFF) : -1;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

        boolean isClosed() {
            return closed;
        }
    }

    /**
     * InputStream that throws IOException on the first read().
     */
    private static class BrokenInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("simulated read failure");
        }
    }

    /**
     * InputStream that succeeds on read() but throws on close().
     */
    private static class CloseFailInputStream extends InputStream {

        private final byte[] payload = "ok".getBytes();

        private int pos = 0;

        private boolean closed = false;

        @Override
        public int read() throws IOException {
            if (closed)
                throw new IOException("already closed");
            return (pos < payload.length) ? (payload[pos++] & 0xFF) : -1;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            throw new IOException("simulated close failure");
        }
    }

    /**
     * ClassLoader that returns a predetermined InputStream or throws an exception.
     */
    private static class StubClassLoader extends ClassLoader {

        private final InputStream streamToReturn;

        private final RuntimeException exceptionToThrow;

        StubClassLoader(InputStream streamToReturn) {
            this.streamToReturn = streamToReturn;
            this.exceptionToThrow = null;
        }

        StubClassLoader(RuntimeException exceptionToThrow) {
            this.streamToReturn = null;
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            // Mimic the behaviour of a real loader returning null for unknown resources
            return (streamToReturn != null && name != null && !name.isEmpty()) ? streamToReturn : null;
        }
    }

    /**
     * Returns the most appropriate {@link ClassLoader} for the given class.
     * The method is deliberately simple – it first tries the thread context
     * class loader and falls back to the class loader that loaded the supplied
     * class (or the system class loader if that is {@code null}).
     *
     * @param clazz a class whose loader is to be used as a fallback
     * @return a {@link ClassLoader} that can be used to load resources
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            return tccl;
        }
        ClassLoader classLoader = (clazz != null) ? clazz.getClassLoader() : null;
        return (classLoader != null) ? classLoader : ClassLoader.getSystemClassLoader();
    }

    /**
     * Returns the default {@link ClassLoader} for this utility class.
     * It simply delegates to {@link #getClassLoader(Class)} using
     * {@code ClassUtilities.class} as the reference class.
     *
     * @return a {@link ClassLoader} suitable for loading resources
     */
    public static ClassLoader getClassLoader() {
        return getClassLoader(ClassUtilities.class);
    }

    /**
     * Loads the content of a class‑path resource as a {@code byte[]} array.
     *
     * @param resourceName the name of the resource to load; may be prefixed
     *                     with a leading '/' which will be stripped.
     * @return the complete byte content of the resource
     * @throws NullPointerException   if {@code resourceName} is {@code null}
     * @throws IllegalArgumentException if the resource cannot be found
     * @throws UncheckedIOException    if an {@link IOException} occurs while
     *                                 reading the resource or closing the stream
     */
    public static byte[] loadResourceAsBytes(String resourceName) {
        // ------------------------------------------------------------
        // 1. Validate the argument
        // ------------------------------------------------------------
        if (resourceName == null) {
            throw new NullPointerException("resourceName cannot be null");
        }
        // Strip a leading slash – ClassLoader#getResourceAsStream does not
        // expect it.
        String normalized = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        // ------------------------------------------------------------
        // 2. Locate the resource
        // ------------------------------------------------------------
        InputStream in = getClassLoader().getResourceAsStream(normalized);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }
        // ------------------------------------------------------------
        // 3. Read the stream fully
        // ------------------------------------------------------------
        try (InputStream autoClose = in;
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            // Wrap any IO problem (including a failure on close) in an
            // UncheckedIOException with a helpful message.
            throw new UncheckedIOException("Error reading resource: " + resourceName, e);
        }
    }

//     @Test
//     @DisplayName("loadResourceAsBytes returns non‑empty byte array for an existing class file")
//     void testLoadExistingResource() {
//         // Using a core JDK class file that is guaranteed to be on the classpath
//         String resourceName = "java/lang/String.class";
//         byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
//         assertNotNull(bytes, "Returned byte array should not be null");
//         assertTrue(bytes.length > 0, "Returned byte array should contain data");
//     }

//     @Test
//     @DisplayName("loadResourceAsBytes throws NullPointerException when resource name is null")
//     void testLoadResourceNullName() {
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsBytes(null), "Null resource name should cause NullPointerException");
//         assertTrue(ex.getMessage().contains("resourceName cannot be null"));
//     }

//     @Test
//     @DisplayName("loadResourceAsBytes throws IllegalArgumentException when resource cannot be found")
//     void testLoadResourceNotFound() {
//         String missingResource = "nonexistent_resource_12345.txt";
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsBytes(missingResource), "Missing resource should cause IllegalArgumentException");
//         assertTrue(ex.getMessage().contains(missingResource), "Exception message should contain the missing resource name");
//     }

//     @Test
//     @DisplayName("IOException during read is wrapped in UncheckedIOException")
//     void testIOExceptionDuringReadPropagation() {
//         // Arrange: a stub loader that returns an InputStream whose read() throws IOException
//         InputStream broken = new BrokenInputStream();
//         ClassLoader stubLoader = new StubClassLoader(broken);
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             // Act & Assert
//             UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsBytes("testdata/broken.txt"));
//             assertTrue(ex.getMessage().contains("Error reading resource: testdata/broken.txt"));
//             assertTrue(ex.getCause() instanceof IOException);
//         }
//     }

//     @ParameterizedTest(name = "Resource not found for \"{0}\"")
//     @CsvSource({ "'nonexistent/resource.txt'", "''" })
//     @DisplayName("Resource not found (including empty string) results in IllegalArgumentException")
//     void testResourceNotFound(String resourceName) {
//         // Stub loader returns null for any name (simulating not found)
//         ClassLoader stubLoader = new StubClassLoader((InputStream) null);
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsBytes(resourceName));
//             assertTrue(ex.getMessage().contains("Resource not found: " + resourceName));
//         }
//     }

//     @Test
//     @DisplayName("Successful read of a small text file")
//     void testSuccessfulReadSmallContent() {
//         // UTF‑8 by default
//         byte[] expected = "small content\n".getBytes();
//         ClassLoader stubLoader = new StubClassLoader(new ByteArrayInputStreamStub(expected));
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             byte[] actual = ClassUtilities.loadResourceAsBytes("testdata/small.txt");
//             assertArrayEquals(expected, actual, "Byte content should match the small file");
//         }
//     }

//     @Test
//     @DisplayName("Successful read of an empty resource returns empty array")
//     void testSuccessfulReadEmptyContent() {
//         ClassLoader stubLoader = new StubClassLoader(new ByteArrayInputStreamStub(new byte[0]));
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             byte[] actual = ClassUtilities.loadResourceAsBytes("testdata/empty.txt");
//             assertNotNull(actual);
//             assertEquals(0, actual.length, "Empty resource should yield zero‑length array");
//         }
//     }

//     @Test
//     @DisplayName("Successful read of a large binary file (1 MiB)")
//     void testSuccessfulReadLargeContent() {
//         // Generate 1 MiB of deterministic data
//         byte[] large = new byte[1024 * 1024];
//         for (int i = 0; i < large.length; i++) {
//             large[i] = (byte) (i % 256);
//         }
//         ClassLoader stubLoader = new StubClassLoader(new ByteArrayInputStreamStub(large));
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             byte[] actual = ClassUtilities.loadResourceAsBytes("testdata/large.bin");
//             assertEquals(large.length, actual.length, "Length should be exactly 1 MiB");
//             assertArrayEquals(large, actual, "Content of the large file must match expected bytes");
//         }
//     }

//     @Test
//     @DisplayName("InputStream is closed exactly once after successful read")
//     void testInputStreamClosedAfterRead() throws Exception {
//         ByteArrayInputStreamStub spyStream = spy(new ByteArrayInputStreamStub("closeable".getBytes()));
//         ClassLoader stubLoader = new StubClassLoader(spyStream);
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             byte[] result = ClassUtilities.loadResourceAsBytes("testdata/closeable.txt");
//             assertNotNull(result);
//             // Verify close() was called once
//             verify(spyStream, times(1)).close();
//             assertTrue(spyStream.isClosed(), "Stream should report as closed");
//         }
//     }

//     @Test
//     @DisplayName("Leading slash in resource name is handled correctly")
//     void testLeadingSlashHandling() {
//         byte[] expected = "small content\n".getBytes();
//         ClassLoader stubLoader = new StubClassLoader(new ByteArrayInputStreamStub(expected));
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             byte[] actual = ClassUtilities.loadResourceAsBytes("/testdata/small.txt");
//             assertArrayEquals(expected, actual, "Resource with leading slash should be resolved");
//         }
//     }

//     @Test
//     @DisplayName("IOException on stream close is wrapped in UncheckedIOException")
//     void testIOExceptionOnClosePropagation() {
//         InputStream stream = new CloseFailInputStream();
//         ClassLoader stubLoader = new StubClassLoader(stream);
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsBytes("testdata/closeable.txt"));
//             assertTrue(ex.getMessage().contains("Error reading resource: testdata/closeable.txt"));
//             assertTrue(ex.getCause() instanceof IOException);
//         }
//     }

//     @Test
//     @DisplayName("SecurityException from ClassLoader propagates unchanged")
//     void testSecurityExceptionPropagation() {
//         RuntimeException securityEx = new SecurityException("access denied");
//         ClassLoader stubLoader = new StubClassLoader(securityEx);
//         try (MockedStatic<ClassUtilities> mock = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
//             mock.when(() -> ClassUtilities.getClassLoader(any())).thenReturn(stubLoader);
//             SecurityException ex = assertThrows(SecurityException.class, () -> ClassUtilities.loadResourceAsBytes("any/resource.txt"));
//             assertEquals("access denied", ex.getMessage());
//         }
//     }
}