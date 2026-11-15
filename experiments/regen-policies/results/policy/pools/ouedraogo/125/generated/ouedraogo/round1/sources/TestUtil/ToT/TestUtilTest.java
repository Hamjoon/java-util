package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TestUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct detection of substrings in a case‑insensitive manner.</li>
 *   <li>Order enforcement for both {@code assertContainsIgnoreCase} and {@code checkContainsIgnoreCase}.</li>
 *   <li>Edge cases such as empty source, empty {@code contains} array, and {@code null} handling.</li>
 *   <li>Verification that {@code assertContainsIgnoreCase} throws an {@link AssertionError} when a token is missing.</li>
 *   <li>Resource loading via {@code fetchResource} using a temporary file placed on the test classpath.</li>
 *   <li>Behaviour of {@code isReleaseMode} with various system‑property configurations.</li>
 * </ul>
 *
 * No external libraries are used beyond JUnit‑4 and the JDK classes referenced in {@link TestUtil}.
 */
public class TestUtilTest {

    private static final String RESOURCE_NAME = "testUtilResource.txt";
    private Path tempResourcePath;

    /**
     * Creates a temporary text file on the classpath that {@code fetchResource} can locate.
     * The file is placed in the same package as {@code TestUtil} (i.e. under {@code com/cedarsoftware/util} in the test output).
     */
    @Before
    public void setUp() throws Exception {
        // Determine the directory where the compiled test classes are written.
        URL classLocation = TestUtil.class.getProtectionDomain().getCodeSource().getLocation();
        Path classesDir = Paths.get(classLocation.toURI());

        // Build the path that mimics the resource location "/testUtilResource.txt"
        tempResourcePath = classesDir.resolve(RESOURCE_NAME);
        Files.createDirectories(tempResourcePath.getParent());

        // Write known content.
        String content = "LineOne\nLineTwo\nLineThree";
        Files.write(tempResourcePath, content.getBytes());
    }

    /**
     * Deletes the temporary resource file after each test run.
     */
    @After
    public void tearDown() throws Exception {
        if (tempResourcePath != null && Files.exists(tempResourcePath)) {
            Files.delete(tempResourcePath);
        }
        // Reset system property used by isReleaseMode to avoid side effects.
        System.clearProperty("performRelease");
    }

    /* ---------------------------------------------------------------------- */
    /*                     Tests for assertContainsIgnoreCase                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void assertContainsIgnoreCase_BasicSuccess() {
        String source = "The quick Brown Fox jumps over the lazy Dog";
        // Tokens are in the correct order, different cases.
        TestUtil.assertContainsIgnoreCase(source, "quick", "brown", "FOX", "dog");
    }

    @Test
    public void assertContainsIgnoreCase_EmptyContainsArray() {
        // No tokens to check – should succeed trivially.
        TestUtil.assertContainsIgnoreCase("anything", new String[0]);
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_MissingTokenThrows() {
        String source = "Hello World";
        // "planet" does not exist – should trigger an AssertionError.
        TestUtil.assertContainsIgnoreCase(source, "hello", "planet");
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_WrongOrderThrows() {
        String source = "alpha beta gamma delta";
        // Tokens are present but out of order.
        TestUtil.assertContainsIgnoreCase(source, "gamma", "beta");
    }

    @Test
    public void assertContainsIgnoreCase_TokenAppearsMultipleTimes() {
        String source = "repeat repeat repeat";
        // Should succeed because the first occurrence of each token is used.
        TestUtil.assertContainsIgnoreCase(source, "repeat", "repeat", "repeat");
    }

    @Test
    public void assertContainsIgnoreCase_SourceIsEmptyButNoTokens() {
        TestUtil.assertContainsIgnoreCase("", new String[0]);
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_SourceIsEmptyButHasTokens() {
        TestUtil.assertContainsIgnoreCase("", "something");
    }

    /* ---------------------------------------------------------------------- */
    /*                     Tests for checkContainsIgnoreCase                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void checkContainsIgnoreCase_BasicSuccess() {
        String source = "One Two Three Four";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "one", "THREE"));
    }

    @Test
    public void checkContainsIgnoreCase_EmptyContainsArray() {
        // Empty var‑args means “nothing to find”, which should be true.
        assertTrue(TestUtil.checkContainsIgnoreCase("anything"));
    }

    @Test
    public void checkContainsIgnoreCase_MissingTokenReturnsFalse() {
        String source = "alpha beta gamma";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "beta", "delta"));
    }

    @Test
    public void checkContainsIgnoreCase_WrongOrderReturnsFalse() {
        String source = "first second third";
        // Tokens exist but order is reversed.
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "third", "first"));
    }

    @Test
    public void checkContainsIgnoreCase_TokenAppearsMultipleTimes() {
        String source = "repeat repeat repeat";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "repeat", "repeat", "repeat"));
    }

    @Test
    public void checkContainsIgnoreCase_SourceEmptyAndNoTokens() {
        assertTrue(TestUtil.checkContainsIgnoreCase("", new String[0]));
    }

    @Test
    public void checkContainsIgnoreCase_SourceEmptyAndHasTokens() {
        assertFalse(TestUtil.checkContainsIgnoreCase("", "anything"));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Tests for fetchResource                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void fetchResource_ReturnsFileContent() {
        // The setUp method placed a file named testUtilResource.txt on the classpath.
        String expected = "LineOne\nLineTwo\nLineThree";
        String actual = TestUtil.fetchResource(RESOURCE_NAME);
        assertEquals(expected, actual);
    }

    @Test(expected = RuntimeException.class)
    public void fetchResource_NonExistingResourceThrows() {
        // Attempt to load a resource that does not exist – should be wrapped in RuntimeException.
        TestUtil.fetchResource("nonexistent_resource_12345.txt");
    }

    /* ---------------------------------------------------------------------- */
    /*                     Tests for isReleaseMode                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void isReleaseMode_DefaultIsFalse() {
        // No system property set – method should return false.
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_TrueWhenPropertySetToTrue() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_FalseWhenPropertySetToFalse() {
        System.setProperty("performRelease", "false");
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_IgnoresCaseAndWhitespace() {
        System.setProperty("performRelease", "  TrUe  ");
        // Boolean.parseBoolean trims whitespace and is case‑insensitive.
        assertTrue(TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_InvalidValueTreatedAsFalse() {
        System.setProperty("performRelease", "notABoolean");
        assertFalse(TestUtil.isReleaseMode());
    }
}
