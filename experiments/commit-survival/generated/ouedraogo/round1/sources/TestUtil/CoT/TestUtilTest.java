package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

/**
 * JUnit‑4 test suite for {@link TestUtil}.
 *
 * NOTE: For the {@code fetchResource} positive test to succeed,
 * a resource file named {@code test-resource.txt} must be placed
 * on the test class‑path (e.g. src/test/resources) with the exact
 * content {@code Hello Resource}.
 */
public class TestUtilTest {

    /* ---------------------------------------------------------------------- */
    /* --------------------------- assertContainsIgnoreCase ----------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void assertContainsIgnoreCase_Pass() {
        // tokens appear in order, case‑insensitive
        TestUtil.assertContainsIgnoreCase("The quick Brown Fox", "quick", "brown", "FOX");
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_FailMissingToken() {
        // "lazy" is not present → AssertionError
        TestUtil.assertContainsIgnoreCase("The quick brown fox", "quick", "lazy");
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_FailWrongOrder() {
        // tokens are present but in wrong order
        TestUtil.assertContainsIgnoreCase("first second third", "third", "first");
    }

    @Test
    public void assertContainsIgnoreCase_EmptyContains() {
        // No tokens to check – should simply return without error
        TestUtil.assertContainsIgnoreCase("any source string");
    }

    @Test(expected = NullPointerException.class)
    public void assertContainsIgnoreCase_NullSource() {
        // source is null → NPE when calling toLowerCase()
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------- checkContainsIgnoreCase ----------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void checkContainsIgnoreCase_Pass() {
        assertTrue(TestUtil.checkContainsIgnoreCase(
                "Java unit testing is fun", "java", "UNIT", "Testing"));
    }

    @Test
    public void checkContainsIgnoreCase_FailMissingToken() {
        assertFalse(TestUtil.checkContainsIgnoreCase(
                "Java unit testing is fun", "java", "missing"));
    }

    @Test
    public void checkContainsIgnoreCase_FailWrongOrder() {
        // tokens exist but order is wrong → false
        assertFalse(TestUtil.checkContainsIgnoreCase(
                "one two three", "three", "one"));
    }

    @Test
    public void checkContainsIgnoreCase_EmptyContains() {
        // No tokens → true by definition
        assertTrue(TestUtil.checkContainsIgnoreCase("any string"));
    }

    @Test(expected = NullPointerException.class)
    public void checkContainsIgnoreCase_NullSource() {
        TestUtil.checkContainsIgnoreCase(null, "anything");
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------------- fetchResource ------------------------ */
    /* ---------------------------------------------------------------------- */

    /**
     * Positive test – reads a known resource from the class‑path.
     * The resource file {@code test-resource.txt} must be placed on the
     * test class‑path with the exact content {@code Hello Resource}.
     */
    @Test
    public void fetchResource_ExistingResource() {
        String expected = "Hello Resource";
        String actual = TestUtil.fetchResource("test-resource.txt");
        assertEquals(expected, actual);
    }

    @Test(expected = RuntimeException.class)
    public void fetchResource_NonExistingResource() {
        // Resource does not exist → RuntimeException is thrown
        TestUtil.fetchResource("non‑existent‑resource.txt");
    }

    @Test(expected = RuntimeException.class)
    public void fetchResource_NullName() {
        // Passing null leads to "/null" lookup → RuntimeException
        TestUtil.fetchResource(null);
    }
}
