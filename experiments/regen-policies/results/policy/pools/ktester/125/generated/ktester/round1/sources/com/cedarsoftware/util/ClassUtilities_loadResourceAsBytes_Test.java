package com.cedarsoftware.util;

import java.util.Objects;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_loadResourceAsBytes_Test {

    private static class ArgumentsHolder {

        final String resourceName;

        final byte[] expectedBytes;

        ArgumentsHolder(String resourceName, byte[] expectedBytes) {
            this.resourceName = resourceName;
            this.expectedBytes = expectedBytes;
        }

        @Override
        public String toString() {
            return resourceName;
        }
    }

//     private static Stream<ArgumentsHolder> normalResourceProvider() {
//         return Stream.of(new ArgumentsHolder("testdata/small.txt", "Hello, World!".getBytes(StandardCharsets.UTF_8)), new ArgumentsHolder("/testdata/small.txt", "Hello, World!".getBytes(StandardCharsets.UTF_8)), new ArgumentsHolder("testdata/file with spaces.txt", "Space content".getBytes(StandardCharsets.UTF_8)), new ArgumentsHolder("testdata/empty.txt", new byte[0]));
//     }

    /**
     * Returns a {@link ClassLoader} that can be used to load resources for the
     * supplied class.  The method prefers the class’s own loader; if that is
     * {@code null} (which can happen for bootstrap classes) the system class
     * loader is returned instead.
     *
     * @param clazz the class whose loader should be used; must not be {@code null}
     * @return a non‑null {@link ClassLoader}
     * @throws NullPointerException if {@code clazz} is {@code null}
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        ClassLoader cl = clazz.getClassLoader();
        return (cl != null) ? cl : ClassLoader.getSystemClassLoader();
    }

    /**
     * Loads the content of a class‑path resource as a byte array.
     *
     * @param resourceName name of the resource to load; may be absolute
     *                     (starting with ‘/’) or relative to the classpath.
     * @return the bytes that constitute the resource
     * @throws NullPointerException   if {@code resourceName} is {@code null}
     * @throws IllegalArgumentException if the resource cannot be found
     * @throws UncheckedIOException    if an {@link IOException} occurs while
     *                                 reading the resource (including errors on
     *                                 {@code close()}).
     */
//     public static byte[] loadResourceAsBytes(String resourceName) {
//         Objects.requireNonNull(resourceName, "resourceName must not be null");
//         // Obtain the appropriate class loader.
//         ClassLoader cl = getClassLoader(ClassUtilities.class);
//         // Locate the resource.
//         InputStream is = cl.getResourceAsStream(resourceName);
//         if (is == null) {
//             // Try again with a leading slash stripped – some callers include it.
//             if (resourceName.startsWith("/")) {
//                 is = cl.getResourceAsStream(resourceName.substring(1));
//             }
//             if (is == null) {
//                 throw new IllegalArgumentException("Resource not found: " + resourceName);
//             }
//         }
//         // Read the stream fully, ensuring that any IOException (including on close)
//         // is wrapped in an UncheckedIOException that mentions the resource name.
//         try (InputStream in = is;
//             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//             byte[] buffer = new byte[8192];
//             int read;
//             while ((read = in.read(buffer)) != -1) {
//                 out.write(buffer, 0, read);
//             }
//             return out.toByteArray();
//         } catch (IOException e) {
//             // Preserve the original exception as the cause.
//             throw new UncheckedIOException("Error reading resource '" + resourceName + "': " + e.getMessage(), e);
//         }
//     }

    /**
     * Verify that a known class‑file resource can be loaded and that the returned
     * byte array matches the bytes obtained directly from the class loader.
     */
//     @Test
//     @DisplayName("loadResourceAsBytes – loads existing resource")
//     void testLoadExistingResource() throws IOException {
//         // The class file for java.lang.String is always present on the classpath.
//         final String resourceName = "java/lang/String.class";
//         byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
//         assertNotNull(bytes, "Returned byte array should not be null");
//         assertTrue(bytes.length > 0, "Returned byte array should contain data");
//         // Read the same resource manually and compare.
//         try (InputStream is = ClassUtilities.getClassLoader(ClassUtilities.class).getResourceAsStream(resourceName)) {
//             assertNotNull(is, "Manual stream should not be null for existing resource");
//             byte[] expected = is.readAllBytes();
//             assertArrayEquals(expected, bytes, "Byte arrays should be identical");
//         }
//     }

    /**
     * Verify that passing {@code null} as the resource name results in a
     * {@link NullPointerException} (as required by {@code Objects.requireNonNull}).
     */
//     @Test
//     @DisplayName("loadResourceAsBytes – null argument")
//     void testLoadResourceNull() {
//         assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsBytes(null), "Null resource name should cause NullPointerException");
//     }

    /**
     * Verify that a non‑existent resource causes an {@link IllegalArgumentException}
     * with an appropriate message.
     */
//     @Test
//     @DisplayName("loadResourceAsBytes – resource not found")
//     void testLoadResourceNotFound() {
//         final String missingResource = "nonexistent/resource.xyz";
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.loadResourceAsBytes(missingResource), "Missing resource should cause IllegalArgumentException");
//         assertTrue(ex.getMessage().contains(missingResource), "Exception message should contain the missing resource name");
//     }

//     @ParameterizedTest(name = "loadResourceAsBytes – {0}")
//     @MethodSource("normalResourceProvider")
//     void testLoadNormalResources(ArgumentsHolder holder) throws IOException {
//         byte[] actual = ClassUtilities.loadResourceAsBytes(holder.resourceName);
//         assertNotNull(actual, "Result should never be null");
//         assertArrayEquals(holder.expectedBytes, actual, () -> "Byte content mismatch for resource: " + holder.resourceName);
//     }

//     @Test
//     @DisplayName("loadResourceAsBytes – Binary Resource")
//     void testLoadBinaryResource() throws IOException {
//         final String resourceName = "testdata/logo.png";
//         // Load expected bytes using the class loader directly.
//         byte[] expected;
//         try (InputStream is = ClassUtilities.getClassLoader(ClassUtilities.class).getResourceAsStream(resourceName)) {
//             assertNotNull(is, "Binary test resource must exist");
//             expected = is.readAllBytes();
//         }
//         byte[] actual = ClassUtilities.loadResourceAsBytes(resourceName);
//         assertNotNull(actual);
//         assertEquals(expected.length, actual.length, "Binary resource length should match expected length");
//         assertArrayEquals(expected, actual, "Binary resource content should be identical");
//     }

//     @Test
//     @DisplayName("loadResourceAsBytes – IOException During Read")
//     void testIOExceptionDuringRead() {
//         final String brokenResource = "testdata/broken.txt";
//         // Mock InputStream that throws IOException on the first read.
//         InputStream faultyStream = new InputStream() {
// 
//             @Override
//             public int read() throws IOException {
//                 throw new IOException("Simulated read failure");
//             }
//         };
//         // Custom ClassLoader that returns the faulty stream for the target resource.
//         ClassLoader mockCl = Mockito.mock(ClassLoader.class);
//         when(mockCl.getResourceAsStream(brokenResource)).thenReturn(faultyStream);
//         try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
//             // Stub the static method that provides the ClassLoader.
//             mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(mockCl);
//             // Ensure the method under test calls the real implementation.
//             mocked.when(() -> ClassUtilities.loadResourceAsBytes(anyString())).thenCallRealMethod();
//             UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsBytes(brokenResource), "IOException during read should be wrapped");
//             assertTrue(ex.getMessage().contains(brokenResource), "Exception message must contain the resource name");
//             assertTrue(ex.getCause() instanceof IOException, "Cause of UncheckedIOException must be an IOException");
//         }
//     }

//     @Test
//     @DisplayName("loadResourceAsBytes – IOException on InputStream close")
//     void testIOExceptionOnClose() throws IOException {
//         final String resourceName = "testdata/close-failure.txt";
//         // InputStream that returns some data then throws on close().
//         byte[] dummyData = "dummy".getBytes(StandardCharsets.UTF_8);
//         InputStream stream = new ByteArrayInputStream(dummyData) {
// 
//             @Override
//             public void close() throws IOException {
//                 throw new IOException("Simulated close failure");
//             }
//         };
//         ClassLoader mockCl = Mockito.mock(ClassLoader.class);
//         when(mockCl.getResourceAsStream(resourceName)).thenReturn(stream);
//         try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
//             mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(mockCl);
//             mocked.when(() -> ClassUtilities.loadResourceAsBytes(anyString())).thenCallRealMethod();
//             UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> ClassUtilities.loadResourceAsBytes(resourceName), "IOException on close should be wrapped");
//             assertTrue(ex.getMessage().contains(resourceName));
//             assertTrue(ex.getCause() instanceof IOException);
//             assertEquals("Simulated close failure", ex.getCause().getMessage());
//         }
//     }

//     @Test
//     @DisplayName("loadResourceAsBytes – SecurityException on ClassLoader")
//     void testSecurityExceptionFromClassLoader() {
//         final String resourceName = "testdata/secure.txt";
//         ClassLoader mockCl = Mockito.mock(ClassLoader.class);
//         when(mockCl.getResourceAsStream(resourceName)).thenThrow(new SecurityException("Access denied"));
//         try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class)) {
//             mocked.when(() -> ClassUtilities.getClassLoader(ClassUtilities.class)).thenReturn(mockCl);
//             mocked.when(() -> ClassUtilities.loadResourceAsBytes(anyString())).thenCallRealMethod();
//             SecurityException ex = assertThrows(SecurityException.class, () -> ClassUtilities.loadResourceAsBytes(resourceName), "SecurityException should propagate unchanged");
//             assertEquals("Access denied", ex.getMessage());
//         }
//     }
}