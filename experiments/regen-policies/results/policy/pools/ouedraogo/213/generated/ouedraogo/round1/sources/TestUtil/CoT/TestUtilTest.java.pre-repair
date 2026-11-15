package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TestUtil}.
 *
 * The suite covers:
 * <ul>
 *   <li>Signature extraction (documented in the comments).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case and exception handling tests.</li>
 * </ul>
 *
 * The file is wrapped between the markers ###Test START## and ###Test END## as required.
 */
public class TestUtilTest {

    /* --------------------------------------------------------------
     *  Public method signatures extracted from TestUtil
     * --------------------------------------------------------------
     *
     * 1. public static void assertContainsIgnoreCase(String source, String... contains)
     *
     * 2. public static boolean checkContainsIgnoreCase(String source, String... contains)
     *
     * 3. public static String fetchResource(String name)
     *
     * 4. public static boolean isReleaseMode()
     *
     * -------------------------------------------------------------- */

    private static final String TEST_RESOURCE_NAME = "test-util-resource.txt";
    private static final String TEST_RESOURCE_CONTENT = "Hello World! This is a Test Resource.";

    /**
     * Helper that creates a temporary resource file on the classpath root.
     * The file is placed under {@code target/test-classes} (the default
     * output directory for test resources) so that {@code TestUtil.class.getResource}
     * can locate it via {@code "/"+name}.
     */
    @Before
    public void createTestResource() throws IOException {
        // Resolve the location where compiled test classes are written.
        URL root = TestUtil.class.getClassLoader().getResource("");
        assertNotNull("Could not locate classpath root for test resources.", root);
        Path rootPath = Paths.get(root.getPath());

        Path resourcePath = rootPath.resolve(TEST_RESOURCE_NAME);
        Files.write(resourcePath, TEST_RESOURCE_CONTENT.getBytes());
    }

    @After
    public void cleanUp() throws IOException {
        // Remove the test resource file.
        URL root = TestUtil.class.getClassLoader().getResource("");
        if (root != null) {
            Path rootPath = Paths.get(root.getPath());
            Files.deleteIfExists(rootPath.resolve(TEST_RESOURCE_NAME));
        }

        // Reset the system property used by isReleaseMode()
        System.clearProperty("performRelease");
    }

    /* --------------------------------------------------------------
     *  Basic functional tests
     * -------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_BasicSuccess() {
        String source = "The quick brown Fox jumps over the lazy dog";
        // Mixed case, order correct
        TestUtil.assertContainsIgnoreCase(source, "quick", "FOX", "lazy");
    }

    @Test
    public void testCheckContainsIgnoreCase_BasicSuccess() {
        String source = "Alpha Beta Gamma Delta";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "alpha", "GAMMA"));
    }

    @Test
    public void testCheckContainsIgnoreCase_BasicFailure() {
        String source = "One Two Three";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "two", "one")); // wrong order
    }

    @Test
    public void testFetchResource_ReturnsFileContent() {
        String content = TestUtil.fetchResource(TEST_RESOURCE_NAME);
        assertEquals(TEST_RESOURCE_CONTENT, content);
    }

    @Test
    public void testIsReleaseMode_DefaultIsFalse() {
        // No system property set – should be false
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_TrueWhenPropertySet() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode());
    }

    /* --------------------------------------------------------------
     *  Edge‑case & exception handling tests
     * -------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_EmptyContainsArray() {
        // Should succeed – nothing to verify
        TestUtil.assertContainsIgnoreCase("any source string");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_MissingTokenThrowsAssertionError() {
        // The token "missing" does not exist in the source; an AssertionError is expected.
        // Note: Java assertions must be enabled for this test to fire. The Maven Surefire
        // plugin (or the IDE) should be configured with the VM option "-ea".
        TestUtil.assertContainsIgnoreCase("some source text", "missing");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullSourceThrowsNPE() {
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullContainsArrayThrowsNPE() {
        // Passing a null var‑args array leads to a NPE when the method tries to iterate.
        TestUtil.assertContainsIgnoreCase("source", (String[]) null);
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyContainsArrayReturnsTrue() {
        assertTrue(TestUtil.checkContainsIgnoreCase("any source"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptySourceAndNoTokens() {
        assertTrue(TestUtil.checkContainsIgnoreCase(""));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptySourceWithTokensReturnsFalse() {
        assertFalse(TestUtil.checkContainsIgnoreCase("", "token"));
    }

    @Test
    public void testCheckContainsIgnoreCase_NullSourceThrowsNPE() {
        try {
            TestUtil.checkContainsIgnoreCase(null, "any");
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // success
        }
    }

    @Test
    public void testCheckContainsIgnoreCase_NullContainsArrayThrowsNPE() {
        try {
            TestUtil.checkContainsIgnoreCase("source", (String[]) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // success
        }
    }

    @Test(expected = RuntimeException.class)
    public void testFetchResource_NonExistingResourceThrowsRuntimeException() {
        // The method wraps any exception into a RuntimeException.
        TestUtil.fetchResource("non‑existent‑resource.txt");
    }

    @Test
    public void testIsReleaseMode_InvalidBooleanValueTreatedAsFalse() {
        System.setProperty("performRelease", "notABoolean");
        // Boolean.parseBoolean returns false for any value other than "true" (ignoring case)
        assertFalse(TestUtil.isReleaseMode());
    }
}
