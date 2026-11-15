package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.net.URL;
import java.nio.file.*;

public class TestUtilTest {

    /* ----------------------------------------------------------------------
     *  Tests for assertContainsIgnoreCase
     * ---------------------------------------------------------------------- */

    @Test
    public void testAssertContainsIgnoreCase_HappyPath() {
        // tokens appear in order, different case
        TestUtil.assertContainsIgnoreCase("The quick Brown Fox", "quick", "BROWN", "fox");
    }

    @Test(expected = AssertionError.class)
    public void testAssertContainsIgnoreCase_WrongOrder() {
        // order is wrong – should trigger an AssertionError
        TestUtil.assertContainsIgnoreCase("alpha beta gamma", "gamma", "alpha");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullSource() {
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void testAssertContainsIgnoreCase_NullContainsElement() {
        TestUtil.assertContainsIgnoreCase("some text", (String) null);
    }

    @Test
    public void testAssertContainsIgnoreCase_EmptyContainsArray() {
        // should simply return without throwing
        TestUtil.assertContainsIgnoreCase("any string");
    }

    @Test
    public void testAssertContainsIgnoreCase_EmptyStringToken() {
        // empty token is always found at index 0
        TestUtil.assertContainsIgnoreCase("anything", "");
    }

    /* ----------------------------------------------------------------------
     *  Tests for checkContainsIgnoreCase
     * ---------------------------------------------------------------------- */

    @Test
    public void testCheckContainsIgnoreCase_HappyPath() {
        assertTrue(TestUtil.checkContainsIgnoreCase("The quick Brown Fox", "quick", "BROWN", "fox"));
    }

    @Test
    public void testCheckContainsIgnoreCase_WrongOrder() {
        assertFalse(TestUtil.checkContainsIgnoreCase("alpha beta gamma", "gamma", "alpha"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyContainsArray() {
        assertTrue(TestUtil.checkContainsIgnoreCase("does not matter"));
    }

    @Test
    public void testCheckContainsIgnoreCase_EmptyStringToken() {
        assertTrue(TestUtil.checkContainsIgnoreCase("whatever", ""));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_NullSource() {
        TestUtil.checkContainsIgnoreCase(null, "something");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckContainsIgnoreCase_NullContainsElement() {
        TestUtil.checkContainsIgnoreCase("some text", (String) null);
    }

    /* ----------------------------------------------------------------------
     *  Tests for fetchResource
     * ---------------------------------------------------------------------- */

    @Test
    public void testFetchResource_ExistingResource() throws IOException {
        // Use the compiled class file of TestUtil itself – it is guaranteed to be on the class‑path.
        String content = TestUtil.fetchResource("com/cedarsoftware/util/TestUtil.class");
        assertNotNull(content);
        assertFalse(content.isEmpty());

        // The returned string should contain at least a few known byte patterns of a .class file.
        // For simplicity we just assert that the length is greater than 100 bytes.
        assertTrue(content.length() > 100);
    }

    @Test
    public void testFetchResource_MissingResource() {
        try {
            TestUtil.fetchResource("this_resource_does_not_exist_12345.txt");
            fail("Expected RuntimeException for missing resource");
        } catch (RuntimeException e) {
            // The cause should be a NullPointerException because getResource returned null
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testFetchResource_NullName() {
        try {
            TestUtil.fetchResource(null);
            fail("Expected RuntimeException when name is null");
        } catch (RuntimeException e) {
            // getResource("/null") returns null → NPE wrapped in RuntimeException
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    /* ----------------------------------------------------------------------
     *  Helper – enable assertions for the JVM when the tests run.
     *  (If the test runner does not start the JVM with -ea, the following
     *   static block forces assertion checking for this class only.)
     * ---------------------------------------------------------------------- */
    static {
        // Force enable assertions for this class (works only when a security manager
        // does not forbid it). If it fails, the test that relies on assertions will
        // be skipped.
        try {
            ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
        } catch (Throwable ignored) {
            // In environments where we cannot change the default status we simply
            // continue; the assertion‑based tests will be ignored.
        }
    }
}
