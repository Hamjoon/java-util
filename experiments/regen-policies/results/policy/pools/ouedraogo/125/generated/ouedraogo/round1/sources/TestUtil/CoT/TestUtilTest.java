package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test cases for {@link TestUtil}.
 *
 * The test class follows the requested steps:
 * 1. Lists all public methods with signatures.
 * 2. Provides basic functional tests.
 * 3. Identifies edge‑case and exception scenarios.
 * 4. Adds dedicated tests for those scenarios.
 * 5. Combines everything into a single JUnit‑4 test file.
 */
public class TestUtilTest {

    /* -----------------------------------------------------------------------
     * 1. PUBLIC METHOD SIGNATURES
     * -----------------------------------------------------------------------
     * public static void assertContainsIgnoreCase(String source, String... contains)
     * public static boolean checkContainsIgnoreCase(String source, String... contains)
     * public static String fetchResource(String name)
     * public static boolean isReleaseMode()
     * ----------------------------------------------------------------------- */

    /* -----------------------------------------------------------------------
     * Helper fields for tests that need a temporary resource file.
     * ----------------------------------------------------------------------- */
    private static final String TEMP_RESOURCE_NAME = "tempTestResource.txt";
    private Path tempResourcePath;

    @Before
    public void setUp() throws Exception {
        // Create a temporary file that will be reachable via the class‑path.
        // The file is placed under the same package as TestUtil (i.e., in
        // the compiled test‑output directory) so that TestUtil.class.getResource
        // can locate it with a leading '/'.
        tempResourcePath = Paths.get("target", "test-classes", TEMP_RESOURCE_NAME);
        Files.createDirectories(tempResourcePath.getParent());
        Files.write(tempResourcePath, "Hello World".getBytes());
    }

    @After
    public void tearDown() throws Exception {
        // Clean up the temporary resource file.
        Files.deleteIfExists(tempResourcePath);
        // Reset the system property that may have been altered by tests.
        System.clearProperty("performRelease");
    }

    /* -----------------------------------------------------------------------
     * 2. BASIC FUNCTIONAL TESTS
     * ----------------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_success() {
        String source = "The quick Brown Fox Jumps Over The Lazy Dog";
        // Order matters, case does not.
        TestUtil.assertContainsIgnoreCase(source, "quick", "BROWN", "jumps", "lazy");
    }

    @Test
    public void testCheckContainsIgnoreCase_true() {
        String source = "Alpha Bravo Charlie Delta";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "alpha", "CHARLIE"));
    }

    @Test
    public void testCheckContainsIgnoreCase_false() {
        String source = "One Two Three";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "two", "four"));
    }

    @Test
    public void testIsReleaseMode_defaultFalse() {
        // No system property set – should be false.
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_trueWhenPropertySet() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode());
    }

    /* -----------------------------------------------------------------------
     * 3. EDGE CASES & EXCEPTION SCENARIOS
     * -----------------------------------------------------------------------
     * - assertContainsIgnoreCase: empty 'contains' array (should pass);
     *   missing token (should trigger AssertionError);
     *   null source (should throw NullPointerException);
     * - checkContainsIgnoreCase: empty 'contains' (should return true);
     *   null source (should throw NullPointerException);
     * - fetchResource: existing resource (happy path);
     *   non‑existing resource (should wrap exception in RuntimeException);
     * - isReleaseMode: malformed boolean value (should be false).
     * ----------------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_emptyContainsArray() {
        // No tokens to verify – the method should simply return without error.
        TestUtil.assertContainsIgnoreCase("anything", new String[0]);
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_missingToken() {
        String source = "Hello world";
        // "planet" does not exist in the source string.
        TestUtil.assertContainsIgnoreCase(source, "hello", "planet");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_nullSource() {
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    @Test
    public void testCheckContainsIgnoreCase_emptyContainsArray() {
        // By definition, an empty token list is always "contained".
        assertTrue(TestUtil.checkContainsIgnoreCase("any string", new String[0]));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_nullSource() {
        TestUtil.checkContainsIgnoreCase(null, "foo");
    }

    @Test
    public void testFetchResource_successfulRead() throws Exception {
        // The temporary file created in setUp() is placed at the root of the test classpath.
        // Its name is TEMP_RESOURCE_NAME.
        String content = TestUtil.fetchResource(TEMP_RESOURCE_NAME);
        assertEquals("Hello World", content);
    }

    @Test(expected = RuntimeException.class)
    public void testFetchResource_nonExistingResource() {
        // Attempt to load a resource that does not exist – should result in a RuntimeException.
        TestUtil.fetchResource("nonexistent_resource_12345.txt");
    }

    @Test
    public void testIsReleaseMode_malformedProperty() {
        System.setProperty("performRelease", "notABoolean");
        // Boolean.parseBoolean returns false for any value other than "true" (ignoring case).
        assertFalse(TestUtil.isReleaseMode());
    }
}
