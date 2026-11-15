package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link TestUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>positive and negative scenarios for {@code assertContainsIgnoreCase}</li>
 *   <li>positive and negative scenarios for {@code checkContainsIgnoreCase}</li>
 *   <li>behaviour of {@code fetchResource} when the resource does not exist</li>
 *   <li>correct handling of the {@code performRelease} system property in {@code isReleaseMode}</li>
 *   <li>edge‑cases such as empty input arrays and null source handling</li>
 * </ul>
 *
 * Note: The {@code assertContainsIgnoreCase} method uses the Java {@code assert}
 * keyword.  These tests assume that the JVM is started with assertions enabled
 * (the {@code -ea} flag).  When assertions are disabled the method does not throw
 * an {@link AssertionError}; in that case the corresponding test will be ignored.
 */
public class TestUtilTest
{
    private static final String ORIGINAL_PERFORM_RELEASE = System.getProperty("performRelease");

    @Before
    public void setUp()
    {
        // Ensure the system property does not interfere with the default test.
        System.clearProperty("performRelease");
    }

    @After
    public void tearDown()
    {
        // Restore original system property value after each test.
        if (ORIGINAL_PERFORM_RELEASE != null)
        {
            System.setProperty("performRelease", ORIGINAL_PERFORM_RELEASE);
        }
        else
        {
            System.clearProperty("performRelease");
        }
    }

    /* --------------------------------------------------------------
     *  Tests for assertContainsIgnoreCase
     * -------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCaseSuccess()
    {
        // All tokens are present and in the correct order (case‑insensitive)
        TestUtil.assertContainsIgnoreCase(
                "The quick Brown fox jumps over the lazy dog",
                "quick", "BROWN", "fox", "OVER", "lazy");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCaseMissingToken()
    {
        // The token "cat" does not appear – an AssertionError should be thrown.
        TestUtil.assertContainsIgnoreCase(
                "The quick brown fox",
                "quick", "cat");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCaseWrongOrder()
    {
        // Tokens are present but out of order – should trigger AssertionError.
        TestUtil.assertContainsIgnoreCase(
                "first middle last",
                "last", "first");
    }

    @Test
    public void testAssertContainsIgnoreCaseEmptyContains()
    {
        // No tokens to check – method should complete without error.
        TestUtil.assertContainsIgnoreCase("any string", new String[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCaseNullSource()
    {
        // Passing a null source causes a NPE when calling toLowerCase().
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    /* --------------------------------------------------------------
     *  Tests for checkContainsIgnoreCase
     * -------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCaseAllFound()
    {
        boolean result = TestUtil.checkContainsIgnoreCase(
                "Alpha beta Gamma delta epsilon",
                "alpha", "GAMMA", "epsilon");
        assertTrue("All tokens should be found in order", result);
    }

    @Test
    public void testCheckContainsIgnoreCaseMissingToken()
    {
        boolean result = TestUtil.checkContainsIgnoreCase(
                "One two three",
                "two", "four");
        assertFalse("Missing token should cause false", result);
    }

    @Test
    public void testCheckContainsIgnoreCaseWrongOrder()
    {
        // Tokens exist but not in the requested order – the method still returns false
        boolean result = TestUtil.checkContainsIgnoreCase(
                "a b c d e",
                "c", "a");
        assertFalse("Wrong order should cause false", result);
    }

    @Test
    public void testCheckContainsIgnoreCaseEmptyContains()
    {
        // Empty var‑args means nothing to verify – should return true.
        assertTrue(TestUtil.checkContainsIgnoreCase("anything"));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCaseNullSource()
    {
        // Null source leads to NPE when toLowerCase() is invoked.
        TestUtil.checkContainsIgnoreCase(null, "any");
    }

    /* --------------------------------------------------------------
     *  Tests for fetchResource
     * -------------------------------------------------------------- */

    @Test(expected = RuntimeException.class)
    public void testFetchResourceMissingResourceThrows()
    {
        // Attempt to fetch a resource that does not exist – should wrap the
        // underlying exception in a RuntimeException.
        TestUtil.fetchResource("non‑existent‑resource.txt");
    }

    /* --------------------------------------------------------------
     *  Tests for isReleaseMode
     * -------------------------------------------------------------- */

    @Test
    public void testIsReleaseModeDefaultIsFalse()
    {
        // By default (no system property) the method must return false.
        assertFalse("Default release mode should be false", TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseModeTrueWhenPropertySet()
    {
        System.setProperty("performRelease", "true");
        assertTrue("Release mode should be true when property is 'true'", TestUtil.isReleaseMode());

        System.setProperty("performRelease", "TrUe"); // case‑insensitive parsing
        assertTrue("Release mode should be true for mixed‑case 'true'", TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseModeFalseWhenPropertySetToFalse()
    {
        System.setProperty("performRelease", "false");
        assertFalse("Release mode should be false when property is 'false'", TestUtil.isReleaseMode());

        System.setProperty("performRelease", "anything-else");
        assertFalse("Non‑boolean values are parsed as false", TestUtil.isReleaseMode());
    }
}
