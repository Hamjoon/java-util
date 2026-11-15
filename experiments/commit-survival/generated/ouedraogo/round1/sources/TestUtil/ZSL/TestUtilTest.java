package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TestUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>{@link TestUtil#assertContainsIgnoreCase(String, String...)} – success and failure cases.</li>
 *   <li>{@link TestUtil#checkContainsIgnoreCase(String, String...)} – various true/false scenarios.</li>
 *   <li>{@link TestUtil#fetchResource(String)} – successful read of a temporary resource and handling of a missing resource.</li>
 * </ul>
 *
 * Note: The {@code assertContainsIgnoreCase} method relies on Java's {@code assert} statement.
 * The test suite assumes that assertions are enabled (e.g., JVM argument {@code -ea} is used).
 */
public class TestUtilTest {

    /** Temporary file name used for the {@code fetchResource} tests. */
    private static final String TEMP_RESOURCE_NAME = "TestUtilTempResource.txt";

    /** Full path to the temporary resource file created under the class‑path root. */
    private Path tempResourcePath;

    /**
     * Creates a temporary file in the class‑path root (the location returned by
     * {@code TestUtil.class.getResource("/")}) so that {@link TestUtil#fetchResource(String)}
     * can locate it via {@code getResource}.
     *
     * @throws Exception if the file cannot be created.
     */
    @Before
    public void setUp() throws Exception {
        // Resolve the class‑path root directory.
        URL rootUrl = TestUtil.class.getResource("/");
        assertNotNull("Class‑path root URL must not be null", rootUrl);
        // Only proceed when the root is a file‑system directory (not a JAR).
        if (!"file".equalsIgnoreCase(rootUrl.getProtocol())) {
            // If the root is not a file system directory, skip resource‑creation tests.
            tempResourcePath = null;
            return;
        }

        Path rootPath = Paths.get(rootUrl.toURI());
        tempResourcePath = rootPath.resolve(TEMP_RESOURCE_NAME);
        Files.write(tempResourcePath, "Temporary resource content".getBytes());
    }

    /**
     * Cleans up the temporary resource file created in {@link #setUp()}.
     *
     * @throws IOException if the file cannot be deleted.
     */
    @After
    public void tearDown() throws IOException {
        if (tempResourcePath != null && Files.exists(tempResourcePath)) {
            Files.delete(tempResourcePath);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for assertContainsIgnoreCase                                    */
    /* ---------------------------------------------------------------------- */

    /** Positive test – the source contains all tokens in the correct order (case‑insensitive). */
    @Test
    public void testAssertContainsIgnoreCaseSuccess() {
        // No exception should be thrown.
        TestUtil.assertContainsIgnoreCase(
                "The quick brown FOX jumps over the lazy dog",
                "quick", "FOX", "LAZY"
        );
    }

    /**
     * Negative test – the source does not contain the tokens in the required order.
     * An {@link AssertionError} is expected when JVM assertions are enabled.
     */
    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCaseFailureWrongOrder() {
        // Tokens appear out of order → should trigger an assertion failure.
        TestUtil.assertContainsIgnoreCase(
                "The quick brown fox jumps over the lazy dog",
                "lazy", "quick"
        );
    }

    /**
     * Negative test – one of the required tokens is absent.
     * An {@link AssertionError} is expected when JVM assertions are enabled.
     */
    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCaseFailureMissingToken() {
        TestUtil.assertContainsIgnoreCase(
                "Hello world!",
                "hello", "planet"
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for checkContainsIgnoreCase                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCaseAllPresentInOrder() {
        assertTrue(TestUtil.checkContainsIgnoreCase(
                "Java unit testing with JUnit",
                "java", "UNIT", "junit"
        ));
    }

    @Test
    public void testCheckContainsIgnoreCaseMissingToken() {
        assertFalse(TestUtil.checkContainsIgnoreCase(
                "Java unit testing with JUnit",
                "java", "missing", "junit"
        ));
    }

    @Test
    public void testCheckContainsIgnoreCaseWrongOrder() {
        // Tokens exist but out of order → should return false.
        assertFalse(TestUtil.checkContainsIgnoreCase(
                "First second third",
                "third", "first"
        ));
    }

    @Test
    public void testCheckContainsIgnoreCaseEmptyContainsArray() {
        // An empty array means “nothing to check”, which should be true.
        assertTrue(TestUtil.checkContainsIgnoreCase("any source string"));
    }

    @Test
    public void testCheckContainsIgnoreCaseCaseInsensitivity() {
        assertTrue(TestUtil.checkContainsIgnoreCase(
                "Case Insensitive MATCH",
                "case", "insensitive", "match"
        ));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for fetchResource                                                */
    /* ---------------------------------------------------------------------- */

    /**
     * Positive test – verifies that a temporary resource written to the class‑path root
     * can be read back via {@link TestUtil#fetchResource(String)}.
     *
     * @throws Exception if the test fails unexpectedly.
     */
    @Test
    public void testFetchResourceSuccess() throws Exception {
        // If the set‑up could not create a file (e.g., running from a JAR), skip this test.
        if (tempResourcePath == null) {
            System.out.println("Skipping fetchResource success test – class‑path root is not a writable directory.");
            return;
        }

        String expected = "Temporary resource content";
        String actual = TestUtil.fetchResource(TEMP_RESOURCE_NAME);
        assertEquals("Content read from the temporary resource should match", expected, actual);
    }

    /**
     * Negative test – attempts to fetch a non‑existent resource and expects a
     * {@link RuntimeException} wrapping the underlying cause.
     */
    @Test(expected = RuntimeException.class)
    public void testFetchResourceMissing() {
        TestUtil.fetchResource("non_existent_resource_12345.txt");
    }
}
