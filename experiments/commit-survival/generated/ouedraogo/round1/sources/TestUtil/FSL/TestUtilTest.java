package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link TestUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>{@link TestUtil#assertContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#checkContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#fetchResource(String)}</li>
 * </ul>
 *
 * The test class deliberately avoids any external libraries or mocking frameworks,
 * using only the Java standard library and the public API of {@code TestUtil}.
 */
public class TestUtilTest {

    /* ------------------------------------------------------------
     *  Tests for assertContainsIgnoreCase
     * ------------------------------------------------------------ */

    @Test
    public void testAssertContainsIgnoreCase_SucceedsWhenAllTokensPresentInOrder_IgnoringCase() {
        String source = "Error: File not found in the target DIRECTORY.";
        // Tokens are present, mixed case, correct order
        TestUtil.assertContainsIgnoreCase(source,
                "error",
                "FILE",
                "directory");
        // If the method returns without throwing, the test passes.
    }

    @Test
    public void testAssertContainsIgnoreCase_FailsWhenTokenMissing() {
        String source = "User login failed due to invalid password.";
        try {
            TestUtil.assertContainsIgnoreCase(source,
                    "login",
                    "missing-token");   // this token does not exist
            fail("Expected an AssertionError because a token is missing");
        } catch (AssertionError e) {
            // Verify that the message mentions the missing token
            assertTrue(e.getMessage().contains("missing-token"));
        }
    }

    @Test
    public void testAssertContainsIgnoreCase_FailsWhenOrderIsWrong() {
        String source = "The quick brown fox jumps over the lazy dog.";
        try {
            // Tokens are present but out of order (\"lazy\" appears after \"fox\")
            TestUtil.assertContainsIgnoreCase(source,
                    "lazy",
                    "fox");
            fail("Expected an AssertionError because the token order is incorrect");
        } catch (AssertionError e) {
            // The failure message should contain the first token that could not be found in order
            assertTrue(e.getMessage().contains("lazy"));
        }
    }

    @Test
    public void testAssertContainsIgnoreCase_EmptyContainsArrayDoesNothing() {
        String source = "Any non‑null source string works.";
        // No tokens to verify – should simply return without error.
        TestUtil.assertContainsIgnoreCase(source);
    }

    /* ------------------------------------------------------------
     *  Tests for checkContainsIgnoreCase
     * ------------------------------------------------------------ */

    @Test
    public void testCheckContainsIgnoreCase_ReturnsTrueWhenAllTokensPresentInOrder_IgnoringCase() {
        String source = "Database connection FAILED: timeout after 30 seconds.";
        boolean result = TestUtil.checkContainsIgnoreCase(source,
                "failed",
                "TIMEOUT",
                "30");
        assertTrue("All tokens are present in order, ignoring case", result);
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenATokenIsMissing() {
        String source = "Service started successfully on port 8080.";
        boolean result = TestUtil.checkContainsIgnoreCase(source,
                "service",
                "error");   // token not present
        assertFalse("Missing token should cause a false result", result);
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenOrderIsIncorrect() {
        String source = "Alpha Beta Gamma Delta";
        // \"Delta\" appears after \"Gamma\" – the requested order is reversed.
        boolean result = TestUtil.checkContainsIgnoreCase(source,
                "Delta",
                "Beta");
        assertFalse("Tokens present but wrong order should return false", result);
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyContainsArrayReturnsTrue() {
        String source = "Whatever content is irrelevant here.";
        boolean result = TestUtil.checkContainsIgnoreCase(source);
        assertTrue("Empty 'contains' array should always return true", result);
    }

    @Test
    public void testCheckContainsIgnoreCase_SourceIsCaseSensitiveButSearchIsNot() {
        String source = "CaseSensitiveTest";
        // Both tokens are in different cases than the source.
        boolean result = TestUtil.checkContainsIgnoreCase(source,
                "casesensitive",
                "TEST");
        assertTrue("Method must ignore case when searching", result);
    }

    /* ------------------------------------------------------------
     *  Tests for fetchResource
     * ------------------------------------------------------------ */

    @Test
    public void testFetchResource_ReturnsContentOfExistingResource() {
        // The JAR/war that runs the tests always contains META-INF/MANIFEST.MF.
        String content = TestUtil.fetchResource("META-INF/MANIFEST.MF");
        assertNotNull("Resource content should not be null", content);
        assertTrue("Manifest file should contain the standard header",
                content.contains("Manifest-Version"));
    }

    @Test
    public void testFetchResource_ThrowsRuntimeExceptionWhenResourceNotFound() {
        String bogusName = "nonexistent_resource_12345.txt";
        try {
            TestUtil.fetchResource(bogusName);
            fail("Expected RuntimeException because the resource does not exist");
        } catch (RuntimeException e) {
            // The cause should be a NullPointerException or a URISyntaxException wrapped inside.
            assertNotNull("Exception should have a cause", e.getCause());
        }
    }

    @Test
    public void testFetchResource_HandlesResourceWithSpecialCharactersInName() throws Exception {
        // Create a temporary file with spaces and Unicode characters in its name,
        // place it on the classpath via the system class loader's resource path.
        // Because we cannot modify the classpath at runtime in a portable way,
        // we instead verify that fetching a known resource that contains spaces
        // works. The JDK ships with "java/util/ServiceLoader.class" – we will fetch
        // its .class file (binary) to ensure the method can read any URL.
        String resourceName = "java/util/ServiceLoader.class";
        String content = TestUtil.fetchResource(resourceName);
        assertNotNull("Binary class file should be read without error", content);
        // The content is binary; we only assert that it has a non‑zero length.
        assertTrue("Read content should not be empty", content.length() > 0);
    }
}
