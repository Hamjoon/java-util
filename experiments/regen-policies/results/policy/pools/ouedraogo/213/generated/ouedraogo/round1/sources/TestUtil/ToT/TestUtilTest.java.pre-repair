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
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TestUtil}.
 *
 * <p>This test class covers the following scenarios for each public method:</p>
 *
 * <ul>
 *   <li><strong>assertContainsIgnoreCase</strong> – successful verification, failure that throws
 *       {@link AssertionError}, handling of empty inputs and {@code null} values.</li>
 *   <li><strong>checkContainsIgnoreCase</strong> – true/false results, order enforcement,
 *       empty/​null arguments.</li>
 *   <li><strong>fetchResource</strong> – successful read of a known resource placed under
 *       <code>src/test/resources</code>, and proper wrapping of errors when the resource does not exist.</li>
 *   <li><strong>isReleaseMode</strong> – default behaviour (no system property) and behaviour when the
 *       {@code performRelease} property is set to {@code true} or {@code false}.</li>
 * </ul>
 *
 * <p>No mocking frameworks are used; only the APIs that appear in the class under test or in the
 * Java standard library are exercised.</p>
 */
public class TestUtilTest {

    /* ---------------------------------------------------------------------- */
    /*  Helpers for fetchResource tests                                        */
    /* ---------------------------------------------------------------------- */

    private static final String TEST_RESOURCE_NAME = "test-util-resource.txt";
    private static final String TEST_RESOURCE_CONTENT = "Hello TestUtil";

    /**
     * Writes a temporary text file to the class‑path root (i.e. the directory that
     * {@code TestUtil.class.getResource("/")} points to).  The file is deleted after
     * all tests have run.
     */
    @Before
    public void createTestResource() throws IOException {
        // Determine the directory that corresponds to the root of the class‑path for TestUtil.
        URL rootUrl = TestUtil.class.getResource("/");
        assertNotNull("Unable to locate class‑path root for TestUtil", rootUrl);
        Path rootPath = new File(rootUrl.getFile()).toPath();

        // Write the test file.
        Path resourcePath = rootPath.resolve(TEST_RESOURCE_NAME);
        Files.write(resourcePath, TEST_RESOURCE_CONTENT.getBytes());

        // Verify that the file is reachable via getResource.
        assertNotNull("Test resource was not found via getResource",
                TestUtil.class.getResource("/" + TEST_RESOURCE_NAME));
    }

    @After
    public void cleanUpTestResource() throws IOException {
        // Remove the temporary file created in @Before.
        URL rootUrl = TestUtil.class.getResource("/");
        if (rootUrl != null) {
            Path rootPath = new File(rootUrl.getFile()).toPath();
            Files.deleteIfExists(rootPath.resolve(TEST_RESOURCE_NAME));
        }

        // Clear the system property used by isReleaseMode() to avoid side effects.
        System.clearProperty("performRelease");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for assertContainsIgnoreCase                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_success() {
        String source = "The quick Brown Fox jumps over the lazy Dog";
        // Should pass – all tokens appear in order, case‑insensitive.
        TestUtil.assertContainsIgnoreCase(source,
                "quick", "brown", "FOX", "jumps", "LAZY", "dog");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_missingToken_throwsAssertionError() {
        String source = "Alpha Beta Gamma";
        // "delta" does not exist → an AssertionError is expected.
        TestUtil.assertContainsIgnoreCase(source, "alpha", "delta");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_wrongOrder_throwsAssertionError() {
        String source = "one two three four";
        // Tokens are present but out of order.
        TestUtil.assertContainsIgnoreCase(source, "three", "two");
    }

    @Test
    public void testAssertContainsIgnoreCase_emptyContainsArray() {
        // No tokens to verify – should succeed trivially.
        TestUtil.assertContainsIgnoreCase("any string");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_nullSource_throwsNPE() {
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_nullToken_throwsNPE() {
        TestUtil.assertContainsIgnoreCase("something", (String) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for checkContainsIgnoreCase                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCase_true() {
        String source = "Spring Summer Autumn Winter";
        assertTrue(TestUtil.checkContainsIgnoreCase(source,
                "spring", "AUTUMN"));
    }

    @Test
    public void testCheckContainsIgnoreCase_false_missingToken() {
        String source = "red green blue";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "yellow"));
    }

    @Test
    public void testCheckContainsIgnoreCase_false_wrongOrder() {
        String source = "first second third";
        // Tokens exist but appear in reverse order.
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "third", "first"));
    }

    @Test
    public void testCheckContainsIgnoreCase_emptyContainsArray() {
        // By definition, an empty token list is trivially contained.
        assertTrue(TestUtil.checkContainsIgnoreCase("any text"));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_nullSource_throwsNPE() {
        TestUtil.checkContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_nullToken_throwsNPE() {
        TestUtil.checkContainsIgnoreCase("something", (String) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for fetchResource                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFetchResource_success() {
        // The @Before method guarantees that the resource file exists with known content.
        String result = TestUtil.fetchResource(TEST_RESOURCE_NAME);
        assertEquals(TEST_RESOURCE_CONTENT, result);
    }

    @Test
    public void testFetchResource_nonExistingResource_throwsRuntimeException() {
        try {
            TestUtil.fetchResource("non‑existent‑resource.txt");
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            // Verify that the cause is a NoSuchFileException (or a subclass thereof).
            assertTrue("Unexpected cause: " + e.getCause(),
                    e.getCause() instanceof java.nio.file.NoSuchFileException ||
                    e.getCause() instanceof java.io.FileNotFoundException);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for isReleaseMode                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsReleaseMode_defaultIsFalse() {
        // No system property set – should return false.
        System.clearProperty("performRelease");
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_explicitTrue() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_explicitFalse() {
        System.setProperty("performRelease", "false");
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_invalidValueTreatedAsFalse() {
        System.setProperty("performRelease", "notABoolean");
        // Boolean.parseBoolean returns false for any string not equal to "true" (ignoring case).
        assertFalse(TestUtil.isReleaseMode());
    }
}
