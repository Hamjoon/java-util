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
 * <p>This class validates the three public static utilities:
 *
 * <ul>
 *   <li>{@link TestUtil#assertContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#checkContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#fetchResource(String)}</li>
 * </ul>
 *
 * <p>All tests are written without any external mocking frameworks – only the
 * public API of {@code TestUtil} and the JDK classes that appear in its source
 * are used.  The resource used by {@code fetchResource} is created dynamically
 * in the test {@code @Before} method and placed on the class‑path via the
 * {@code target/test-classes} directory (the default output folder for Maven/Gradle
 * test compilation).  The file is deleted in {@code @After} to keep the test
 * environment clean.
 */
public class TestUtilTest
{
    /** Name of the temporary resource file used by {@link #testFetchResource_success()} */
    private static final String RESOURCE_NAME = "testutil_sample.txt";

    /** Full path to the temporary resource file on the file system */
    private Path resourcePath;

    /** Content written to the temporary resource file */
    private static final String RESOURCE_CONTENT = "LineOne\nLineTwo\nLineThree";

    /**
     * Creates a temporary file on the class‑path that {@link TestUtil#fetchResource(String)}
     * can locate using {@code TestUtil.class.getResource("/")}.
     *
     * @throws IOException if the file cannot be created or written
     */
    @Before
    public void setUp() throws IOException
    {
        // Resolve the directory that corresponds to the root of the test class‑path.
        URL rootUrl = TestUtil.class.getResource("/");
        assertNotNull("Unable to locate class‑path root for test resources", rootUrl);
        Path rootPath = Paths.get(rootUrl.getPath());

        // Create the temporary file inside that directory.
        resourcePath = rootPath.resolve(RESOURCE_NAME);
        Files.write(resourcePath, RESOURCE_CONTENT.getBytes());
    }

    /**
     * Cleans up the temporary resource file after each test.
     *
     * @throws IOException if the file cannot be deleted
     */
    @After
    public void tearDown() throws IOException
    {
        if (resourcePath != null && Files.exists(resourcePath))
        {
            Files.delete(resourcePath);
        }
    }

    /* -------------------------------------------------------------
     *  Tests for assertContainsIgnoreCase
     * ------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_basicSuccess()
    {
        String source = "The quick Brown Fox jumps over the lazy Dog";
        // The tokens are present in the correct order, case does not matter.
        TestUtil.assertContainsIgnoreCase(source, "quick", "BROWN", "dog");
    }

    @Test
    public void testAssertContainsIgnoreCase_multipleOccurrences()
    {
        String source = "abc abc abc";
        // Ensure that the method advances the search window after each match.
        TestUtil.assertContainsIgnoreCase(source, "abc", "abc", "abc");
    }

    @Test
    public void testAssertContainsIgnoreCase_singleToken()
    {
        TestUtil.assertContainsIgnoreCase("HelloWorld", "world");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_missingToken()
    {
        TestUtil.assertContainsIgnoreCase("Hello World", "goodbye");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_wrongOrder()
    {
        // Tokens are present but in the wrong order.
        TestUtil.assertContainsIgnoreCase("first middle last", "last", "first");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_nullSource()
    {
        // Passing null will cause a NullPointerException before the assertion,
        // which is acceptable – the contract does not define null handling.
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    /* -------------------------------------------------------------
     *  Tests for checkContainsIgnoreCase
     * ------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCase_allPresent()
    {
        String source = "Alpha Beta Gamma Delta";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "alpha", "GAMMA"));
    }

    @Test
    public void testCheckContainsIgnoreCase_missingToken()
    {
        String source = "One Two Three";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "two", "four"));
    }

    @Test
    public void testCheckContainsIgnoreCase_wrongOrder()
    {
        String source = "first second third";
        // Tokens exist but not in the requested order.
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "third", "first"));
    }

    @Test
    public void testCheckContainsIgnoreCase_emptyContainsArray()
    {
        // With no tokens to check, the method should trivially return true.
        assertTrue(TestUtil.checkContainsIgnoreCase("any string"));
    }

    @Test
    public void testCheckContainsIgnoreCase_duplicateTokens()
    {
        String source = "repeat repeat repeat";
        assertTrue(TestUtil.checkContainsIgnoreCase(source, "repeat", "repeat", "repeat"));
    }

    @Test
    public void testCheckContainsIgnoreCase_nullSource()
    {
        // Null source leads to NullPointerException – the method does not guard against it.
        try
        {
            TestUtil.checkContainsIgnoreCase(null, "anything");
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) { }
    }

    /* -------------------------------------------------------------
     *  Tests for fetchResource
     * ------------------------------------------------------------- */

    @Test
    public void testFetchResource_success()
    {
        // fetchResource should read the exact content we wrote in setUp().
        String result = TestUtil.fetchResource(RESOURCE_NAME);
        assertEquals("Resource content must match what was written", RESOURCE_CONTENT, result);
    }

    @Test(expected = RuntimeException.class)
    public void testFetchResource_resourceNotFound()
    {
        // Passing a name that does not exist forces the method to throw RuntimeException.
        TestUtil.fetchResource("nonexistent_resource_12345.txt");
    }

    @Test
    public void testFetchResource_emptyFile()
    throws IOException
    {
        // Create an empty file on the class‑path.
        String emptyName = "empty.txt";
        Path emptyPath = Paths.get(TestUtil.class.getResource("/").getPath()).resolve(emptyName);
        Files.createFile(emptyPath);
        try
        {
            String result = TestUtil.fetchResource(emptyName);
            assertEquals("Empty file should return an empty string", "", result);
        }
        finally
        {
            Files.deleteIfExists(emptyPath);
        }
    }

    @Test
    public void testFetchResource_largeFile()
    throws IOException
    {
        // Build a relatively large string (~100KB) to ensure stream handling works.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        String largeContent = sb.toString();

        String largeName = "large.txt";
        Path largePath = Paths.get(TestUtil.class.getResource("/").getPath()).resolve(largeName);
        Files.write(largePath, largeContent.getBytes());

        try
        {
            String result = TestUtil.fetchResource(largeName);
            assertEquals("Large file content must be read completely", largeContent, result);
        }
        finally
        {
            Files.deleteIfExists(largePath);
        }
    }
}
