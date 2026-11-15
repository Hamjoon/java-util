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
 *   <li>assertContainsIgnoreCase – successful verification, failure due to missing token,
 *       and failure due to wrong order.</li>
 *   <li>checkContainsIgnoreCase – true/false outcomes for the same scenarios.</li>
 *   <li>fetchResource – reading a known resource from the class‑path.</li>
 *   <li>isReleaseMode – behaviour based on the {@code performRelease} system property.</li>
 * </ul>
 */
public class TestUtilTest {

    private static final String RESOURCE_NAME = "test-resource.txt";
    private static final String RESOURCE_CONTENT = "Hello, TestUtil!\nSecond line.";

    /**
     * Ensure the system property used by {@link TestUtil#isReleaseMode()} is cleared before each test.
     */
    @Before
    public void setUp() {
        System.clearProperty("performRelease");
    }

    @After
    public void tearDown() {
        System.clearProperty("performRelease");
    }

    /* ------------------------------------------------------------
     *  Tests for assertContainsIgnoreCase
     * ------------------------------------------------------------ */

    @Test
    public void testAssertContainsIgnoreCase_PassesWhenAllTokensPresentInOrder_IgnoringCase() {
        String source = "The quick Brown FOX jumps over the lazy Dog.";
        // tokens are deliberately mixed‑case and spaced differently
        TestUtil.assertContainsIgnoreCase(source, "quick", "brown", "fox", "LAZY", "dog");
    }

    @Test
    public void testAssertContainsIgnoreCase_PassesWithNoTokens() {
        // An empty var‑args array should never fail.
        TestUtil.assertContainsIgnoreCase("any string");
    }

    @Test
    public void testAssertContainsIgnoreCase_FailsWhenTokenMissing() {
        String source = "Alpha Beta Gamma";
        try {
            TestUtil.assertContainsIgnoreCase(source, "beta", "delta");
            fail("Expected an AssertionError because 'delta' is not in the source");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("'delta' not found"));
        }
    }

    @Test
    public void testAssertContainsIgnoreCase_FailsWhenOrderIsWrong() {
        String source = "one two three four";
        try {
            // 'four' appears after 'three' in the source, but we ask for 'four' first.
            TestUtil.assertContainsIgnoreCase(source, "four", "three");
            fail("Expected an AssertionError because the order is incorrect");
        } catch (AssertionError e) {
            // The message will refer to the first token that cannot be found in the expected order.
            assertTrue(e.getMessage().contains("'four' not found"));
        }
    }

    /* ------------------------------------------------------------
     *  Tests for checkContainsIgnoreCase
     * ------------------------------------------------------------ */

    @Test
    public void testCheckContainsIgnoreCase_ReturnsTrueWhenAllTokensPresentInOrder() {
        String source = "Spring Summer Autumn Winter";
        assertTrue(TestUtil.checkContainsIgnoreCase(source,
                "spring", "AUTUMN", "winter"));
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsTrueWithEmptyTokenArray() {
        assertTrue(TestUtil.checkContainsIgnoreCase("anything"));
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenTokenMissing() {
        String source = "cat dog mouse";
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "dog", "elephant"));
    }

    @Test
    public void testCheckContainsIgnoreCase_ReturnsFalseWhenOrderIsWrong() {
        String source = "a b c d e";
        // 'e' occurs after 'c', but we request it before 'c'.
        assertFalse(TestUtil.checkContainsIgnoreCase(source, "e", "c"));
    }

    /* ------------------------------------------------------------
     *  Tests for fetchResource
     * ------------------------------------------------------------ */

    /**
     * The test resource {@value #RESOURCE_NAME} must be placed in the same
     * package (or in the test resources root) so that {@code TestUtil}
     * can locate it via {@code getResource("/")}.
     *
     * The file contains the exact text defined in {@link #RESOURCE_CONTENT}.
     */
    @Test
    public void testFetchResource_ReturnsExactFileContents() {
        // The resource file is bundled with the test sources.
        String content = TestUtil.fetchResource(RESOURCE_NAME);
        // Normalise line endings for platforms that use "\r\n".
        String normalized = content.replace("\r\n", "\n");
        assertEquals(RESOURCE_CONTENT, normalized);
    }

    @Test(expected = RuntimeException.class)
    public void testFetchResource_ThrowsRuntimeExceptionWhenResourceMissing() {
        // Attempt to load a non‑existent resource – the implementation wraps the
        // underlying exception in a RuntimeException.
        TestUtil.fetchResource("non‑existent‑resource.txt");
    }

    /* ------------------------------------------------------------
     *  Tests for isReleaseMode
     * ------------------------------------------------------------ */

    @Test
    public void testIsReleaseMode_DefaultsToFalse() {
        // No system property set → false
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_TrueWhenPropertySetToTrue() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_FalseWhenPropertySetToFalse() {
        System.setProperty("performRelease", "false");
        assertFalse(TestUtil.isReleaseMode());
    }

    @Test
    public void testIsReleaseMode_FalseWhenPropertyIsNonBoolean() {
        System.setProperty("performRelease", "notABoolean");
        // Boolean.parseBoolean returns false for any value other than "true" (ignoring case)
        assertFalse(TestUtil.isReleaseMode());
    }
}
