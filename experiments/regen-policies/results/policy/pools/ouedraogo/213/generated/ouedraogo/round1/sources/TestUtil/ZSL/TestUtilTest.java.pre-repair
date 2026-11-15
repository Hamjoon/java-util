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

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TestUtil}.
 *
 * <p>All public static methods of {@code TestUtil} are exercised:
 * <ul>
 *   <li>{@link TestUtil#assertContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#checkContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#fetchResource(String)}</li>
 *   <li>{@link TestUtil#isReleaseMode()}</li>
 * </ul>
 *
 * <p>Note: the {@code assert} statements inside {@code assertContainsIgnoreCase}
 * are only evaluated when JVM assertions are enabled (the {@code -ea} flag).  The
 * test that expects an {@link AssertionError} therefore requires assertions to be
 * active.  Most build tools (Maven Surefire, Gradle test) enable them by default;
 * otherwise the test can be run manually with {@code java -ea …}.
 */
public class TestUtilTest
{
    private static final String RESOURCE_NAME = "testresource.txt";
    private static final String RESOURCE_CONTENT = "Hello World! This is a Test Resource.";

    private String originalPerformReleaseProperty;

    /**
     * Creates a temporary resource file on the classpath root.  The file is placed
     * under {@code target/test-classes} (or the equivalent directory used by the
     * test runner) so that {@code TestUtil.class.getResource("/" + name)} can locate it.
     */
    @Before
    public void setUp() throws IOException {
        // Preserve original system property for later restoration.
        originalPerformReleaseProperty = System.getProperty("performRelease");

        // Write the resource file to the same directory where the compiled test classes live.
        // This works because the test classes are loaded from the classpath root.
        URL classRoot = TestUtil.class.getClassLoader().getResource("");
        assertNotNull("Unable to locate classpath root for resource creation.", classRoot);
        Path resourcePath = Paths.get(classRoot.getPath(), RESOURCE_NAME);
        Files.write(resourcePath, RESOURCE_CONTENT.getBytes());
    }

    @After
    public void tearDown() throws IOException {
        // Restore the system property to its original state.
        if (originalPerformReleaseProperty != null) {
            System.setProperty("performRelease", originalPerformReleaseProperty);
        } else {
            System.clearProperty("performRelease");
        }

        // Delete the temporary resource file.
        URL classRoot = TestUtil.class.getClassLoader().getResource("");
        if (classRoot != null) {
            Path resourcePath = Paths.get(classRoot.getPath(), RESOURCE_NAME);
            Files.deleteIfExists(resourcePath);
        }
    }

    /* ----------------------------------------------------------------------
     * Tests for assertContainsIgnoreCase
     * ---------------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_PassesWhenAllTokensPresentInOrder_IgnoringCase() {
        String source = "The quick Brown FOX jumps over the lazy dog";
        // Tokens are deliberately mixed‑case and not contiguous.
        TestUtil.assertContainsIgnoreCase(source,
                "quick",   // present
                "brown",   // present, different case
                "fox",     // present, different case
                "LAZY");   // present, different case
        // No exception means success.
    }

    @Test
    public void testAssertContainsIgnoreCase_PassesWhenNoTokensProvided() {
        // Empty var‑args should be a no‑op and never fail.
        TestUtil.assertContainsIgnoreCase("any string");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_FailsWhenTokenMissing() {
        String source = "Hello world";
        // "planet" does not exist → should trigger an AssertionError.
        TestUtil.assertContainsIgnoreCase(source, "hello", "planet");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_FailsWhenOrderIsWrong() {
        String source = "first middle last";
        // "last" appears before "middle" in the argument list → should fail.
        TestUtil.assertContainsIgnoreCase(source, "first", "last", "middle");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullSourceThrowsNPE() {
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullTokenThrowsNPE() {
        TestUtil.assertContainsIgnoreCase("some text", (String) null);
    }

    /* ----------------------------------------------------------------------
     * Tests for checkContainsIgnoreCase
     * ---------------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCase_ReturnsTrueWhenAllTokensPresentInOrder_IgnoringCase() {
        String source = "Alpha Beta Gamma Delta";
        assertTrue(TestUtil.checkContainsIgnoreCase(source,
                "alpha", "BETA", "gamma"));
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenTokenMissing() {
        String source = "One Two Three";
        assertFalse(TestUtil.checkContainsIgnoreCase(source,
                "one", "four")); // "four" not present
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenOrderIsIncorrect() {
        String source = "a b c d e";
        // "e" appears after "c" in the source, but we ask for "e" before "c".
        assertFalse(TestUtil.checkContainsIgnoreCase(source,
                "e", "c"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyVarargsReturnsTrue() {
        assertTrue(TestUtil.checkContainsIgnoreCase("anything"));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_NullSourceThrowsNPE() {
        TestUtil.checkContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_NullTokenThrowsNPE() {
        TestUtil.checkContainsIgnoreCase("some text", (String) null);
    }

    /* ----------------------------------------------------------------------
     * Tests for fetchResource
     * ---------------------------------------------------------------------- */

    @Test
    public void testFetchResource_ReturnsExactFileContent() {
        // The resource was created in @Before and placed on the classpath root.
        String content = TestUtil.fetchResource(RESOURCE_NAME);
        assertEquals("Fetched resource content should match the file content.",
                RESOURCE_CONTENT, content);
    }

    @Test(expected = RuntimeException.class)
    public void testFetchResource_ThrowsRuntimeExceptionWhenResourceMissing() {
        // Attempt to fetch a non‑existent resource.
        TestUtil.fetchResource("nonexistent_resource_12345.txt");
    }

    /* ----------------------------------------------------------------------
     * Tests for isReleaseMode
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsReleaseMode_DefaultsToFalseWhenPropertyAbsent() {
        System.clearProperty("performRelease");
        assertFalse("When the system property is not set, release mode must be false.",
                TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_ParsesTrueValue() {
        System.setProperty("performRelease", "true");
        assertTrue("System property 'performRelease' set to true should enable release mode.",
                TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_ParsesFalseValue() {
        System.setProperty("performRelease", "false");
        assertFalse("System property 'performRelease' set to false should disable release mode.",
                TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_IgnoresCaseAndNonBooleanValues() {
        // Any value other than "true" (ignoring case) yields false.
        System.setProperty("performRelease", "TrUe");
        assertTrue(TestUtil.isReleaseMode());

        System.setProperty("performRelease", "yes");
        assertFalse(TestUtil.isReleaseMode());

        System.setProperty("performRelease", "1");
        assertFalse(TestUtil.isReleaseMode());
    }
}
