package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TestUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>{@link TestUtil#assertContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#checkContainsIgnoreCase(String, String...)}</li>
 *   <li>{@link TestUtil#fetchResource(String)}</li>
 *   <li>{@link TestUtil#isReleaseMode()}</li>
 * </ul>
 *
 * NOTE: The {@code assertContainsIgnoreCase} method uses the Java {@code assert}
 * statement.  To make the failure test reliable the JVM must be started with
 * assertions enabled ( {@code -ea} ).  Most IDEs and build tools enable this
 * for test execution; if not, the failure test will be skipped.
 */
public class TestUtilTest {

    private static final String SAMPLE_TEXT = "The quick Brown Fox jumps over the lazy Dog";

    private String originalPerformRelease;

    @Before
    public void saveSystemProperty() {
        // Preserve the original value (may be null) so we can restore it later.
        originalPerformRelease = System.getProperty("performRelease");
    }

    @After
    public void restoreSystemProperty() {
        // Restore the original system property to avoid side effects for other tests.
        if (originalPerformRelease != null) {
            System.setProperty("performRelease", originalPerformRelease);
        } else {
            System.clearProperty("performRelease");
        }
    }

    /* -----------------------------------------------------------
     *  Tests for assertContainsIgnoreCase
     * ----------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_SucceedsWhenAllTokensPresentInOrder() {
        // All tokens are present and appear in the correct order, case‑insensitive.
        TestUtil.assertContainsIgnoreCase(
                SAMPLE_TEXT,
                "quick",
                "brown",
                "FOX",
                "jumps",
                "LAZY",
                "dog"
        );
        // No exception means the test passes.
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_FailsWhenTokenMissing() {
        // The token "cat" does not exist in the source string, so an AssertionError is expected.
        TestUtil.assertContainsIgnoreCase(SAMPLE_TEXT, "quick", "cat", "dog");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_FailsWhenOrderIsWrong() {
        // All tokens exist but "dog" appears before "fox" in the argument list.
        TestUtil.assertContainsIgnoreCase(SAMPLE_TEXT, "dog", "fox");
    }

    /* -----------------------------------------------------------
     *  Tests for checkContainsIgnoreCase
     * ----------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCase_ReturnsTrueForValidSequence() {
        assertTrue(TestUtil.checkContainsIgnoreCase(
                SAMPLE_TEXT,
                "THE",
                "quick",
                "brown",
                "FOX",
                "jumps",
                "OVER",
                "lazy",
                "dog"));
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenTokenMissing() {
        assertFalse(TestUtil.checkContainsIgnoreCase(SAMPLE_TEXT, "quick", "cat", "dog"));
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenOrderIsWrong() {
        // Tokens exist but the required order is not respected.
        assertFalse(TestUtil.checkContainsIgnoreCase(SAMPLE_TEXT, "dog", "fox"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyContainsArrayAlwaysTrue() {
        // No tokens to check – should trivially return true.
        assertTrue(TestUtil.checkContainsIgnoreCase(SAMPLE_TEXT));
    }

    @Test
    public void testCheckContainsIgnoreCase_SourceIsEmpty() {
        // Empty source string cannot contain any non‑empty token.
        assertFalse(TestUtil.checkContainsIgnoreCase("", "anything"));
        // But with no tokens it returns true.
        assertTrue(TestUtil.checkContainsIgnoreCase(""));
    }

    /* -----------------------------------------------------------
     *  Tests for fetchResource
     * ----------------------------------------------------------- */

    @Test
    public void testFetchResource_ReturnsContentOfExistingResource() {
        // The class file of TestUtil is guaranteed to be on the classpath.
        // We use its binary content – we only verify that something is returned.
        String content = TestUtil.fetchResource("com/cedarsoftware/util/TestUtil.class");
        assertNotNull("Resource content must not be null", content);
        assertTrue("Resource content should have a positive length", content.length() > 0);
    }

    @Test
    public void testFetchResource_ThrowsRuntimeExceptionWhenResourceMissing() {
        try {
            TestUtil.fetchResource("nonexistent_resource_12345.txt");
            fail("Expected RuntimeException because the resource does not exist");
        } catch (RuntimeException e) {
            // Expected path – verify that the cause is a NullPointerException or
            // any other exception caused by missing resource.
            assertNotNull("Exception cause should be present", e.getCause());
        }
    }

    /* -----------------------------------------------------------
     *  Tests for isReleaseMode
     * ----------------------------------------------------------- */

    @Test
    public void testIsReleaseMode_DefaultIsFalse() {
        // Ensure the system property is not set.
        System.clearProperty("performRelease");
        assertFalse("When the property is absent, release mode must be false", TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_TrueWhenPropertySetToTrue() {
        System.setProperty("performRelease", "true");
        assertTrue("Release mode should be true when property is 'true'", TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_FalseWhenPropertySetToFalse() {
        System.setProperty("performRelease", "false");
        assertFalse("Release mode should be false when property is 'false'", TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_IgnoresCaseAndNonBooleanValues() {
        System.setProperty("performRelease", "TrUe");
        assertTrue(TestUtil.isReleaseMode());

        System.setProperty("performRelease", "yes"); // not a valid boolean, parses as false
        assertFalse(TestUtil.isReleaseMode());
    }
}
