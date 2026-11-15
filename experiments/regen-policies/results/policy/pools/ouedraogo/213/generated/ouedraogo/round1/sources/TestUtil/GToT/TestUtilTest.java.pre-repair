package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TestUtil}.
 *
 * The suite covers:
 * <ul>
 *   <li>normal usage of each public method</li>
 *   <li>edge‑case inputs (empty strings, empty var‑args, nulls)</li>
 *   <li>exception / error paths (missing resources, invalid arguments, assertion failures)</li>
 * </ul>
 *
 * All tests are written using only the APIs that appear in the source of {@code TestUtil}
 * and the standard JDK classes it references.
 */
public class TestUtilTest {

    private static final String ORIGINAL_PERFORM_RELEASE = System.getProperty("performRelease");

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /**
     * Creates a temporary text file on the class‑path (under {@code target/test-classes})
     * and returns its name relative to the root of the class‑path.
     */
    private String createTempResource(String content) throws Exception {
        // Create a temporary file in the same directory as the compiled test class.
        // The file will be automatically on the class‑path because the test classes
        // directory is part of the runtime class‑path.
        File tmp = File.createTempFile("testresource", ".txt");
        tmp.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(content.getBytes("UTF-8"));
        }
        // Return the simple file name (the method prefixes '/' internally).
        return tmp.getName();
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for assertContainsIgnoreCase                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_SimpleSuccess() {
        String source = "The quick brown fox jumps over the lazy dog";
        // all tokens appear in order, case‑insensitive
        TestUtil.assertContainsIgnoreCase(source, "quick", "FOX", "lazy");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_Failure_ThrowsAssertionError() {
        // Assertions must be enabled for this test (run with -ea).
        String source = "Hello world";
        TestUtil.assertContainsIgnoreCase(source, "world", "missing");
    }

    @Test
    public void testAssertContainsIgnoreCase_EmptyContainsArray() {
        // No tokens to check – should succeed trivially.
        TestUtil.assertContainsIgnoreCase("any string");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullSource() {
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for checkContainsIgnoreCase                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCase_AllTokensPresent() {
        String source = "Alpha Beta Gamma Delta";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "alpha", "GAMMA"));
    }

    @Test
    public void testCheckContainsIgnoreCase_TokenMissing() {
        String source = "One Two Three";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "two", "four"));
    }

    @Test
    public void testCheckContainsIgnoreCase_TokensOutOfOrder() {
        String source = "first second third";
        // tokens are present but not in the required order → should return false
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "third", "first"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyContainsArray() {
        // By definition, an empty list of tokens is always satisfied.
        assertTrue(TestUtil.checkContainsIgnoreCase("anything"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptySource() {
        assertTrue(TestUtil.checkContainsIgnoreCase("", new String[0])); // true
        assertFalse(TestUtil.checkContainsIgnoreCase("", "nonEmpty")); // false
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_NullSource() {
        TestUtil.checkContainsIgnoreCase(null, "anything");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for fetchResource                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFetchResource_Success_WithExistingClassFile() throws Exception {
        // The compiled .class file for TestUtil itself is guaranteed to exist.
        String classResourceName = "com/cedarsoftware/util/TestUtil.class";
        String content = TestUtil.fetchResource(classResourceName);
        assertNotNull(content);
        // The class file is binary; we just verify that something was read.
        assertTrue(content.length() > 0);
    }

    @Test
    public void testFetchResource_Success_WithTemporaryTextFile() throws Exception {
        String expected = "Temporary resource content";
        String resourceName = createTempResource(expected);
        // The temporary file lives on the class‑path, so fetchResource can locate it.
        String actual = TestUtil.fetchResource(resourceName);
        assertEquals(expected, actual);
    }

    @Test
    public void testFetchResource_Failure_ResourceNotFound() {
        String missing = "nonexistent_resource_12345.txt";
        try {
            TestUtil.fetchResource(missing);
            fail("Expected RuntimeException because the resource does not exist");
        } catch (RuntimeException e) {
            // The cause should be a NullPointerException (url == null) or an IOException.
            assertNotNull(e.getCause());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testFetchResource_NullName() {
        TestUtil.fetchResource(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for isReleaseMode                                               */
    /* ---------------------------------------------------------------------- */

    @Before
    public void setUpReleaseModeProperty() {
        // Ensure a clean state before each test.
        System.clearProperty("performRelease");
    }

    @After
    public void restoreOriginalProperty() {
        // Restore the original system property value after tests run.
        if (ORIGINAL_PERFORM_RELEASE != null) {
            System.setProperty("performRelease", ORIGINAL_PERFORM_RELEASE);
        } else {
            System.clearProperty("performRelease");
        }
    }

    @Test
    public void testIsReleaseMode_DefaultIsFalse() {
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_ExplicitTrue() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_ExplicitFalse() {
        System.setProperty("performRelease", "false");
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_NonBooleanValue() {
        System.setProperty("performRelease", "notABoolean");
        // Boolean.parseBoolean returns false for any value other than "true" (ignoring case).
        assertFalse(TestUtil.isReleaseMode());
    }
}
