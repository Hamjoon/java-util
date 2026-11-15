package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TestUtil}.
 *
 * The suite covers:
 * <ul>
 *   <li>Normal / typical usage of each public method</li>
 *   <li>Edge‑case situations (empty arguments, null values, wrong order, etc.)</li>
 *   <li>Exception / error handling paths</li>
 * </ul>
 *
 * <strong>NOTE:</strong> a test resource file <code>test.txt</code> containing the text
 * {@code Hello Resource} must be placed on the test class‑path (e.g. <code>src/test/resources</code>).
 */
public class TestUtilTest
{
    /* ---------------------------------------------------------------------- */
    /*  Helpers – restore system properties after each test                     */
    /* ---------------------------------------------------------------------- */
    private static final String RELEASE_PROP = "performRelease";
    private String originalReleaseProp;

    @Before
    public void saveSystemProperty()
    {
        originalReleaseProp = System.getProperty(RELEASE_PROP);
    }

    @After
    public void restoreSystemProperty()
    {
        if (originalReleaseProp == null) {
            System.clearProperty(RELEASE_PROP);
        } else {
            System.setProperty(RELEASE_PROP, originalReleaseProp);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for assertContainsIgnoreCase                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void assertContainsIgnoreCase_HappyPath()
    {
        // tokens appear in the correct order, case‑insensitive
        TestUtil.assertContainsIgnoreCase("The quick Brown Fox Jumps", "quick", "BROWN", "fox");
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_MissingToken()
    {
        // "lazy" is not present – should trigger an AssertionError
        TestUtil.assertContainsIgnoreCase("The quick brown fox", "quick", "lazy");
    }

    @Test(expected = AssertionError.class)
    public void assertContainsIgnoreCase_WrongOrder()
    {
        // tokens are present but out of order – the second token will not be found after the first
        TestUtil.assertContainsIgnoreCase("alpha beta gamma", "gamma", "beta");
    }

    @Test
    public void assertContainsIgnoreCase_EmptyContainsArray()
    {
        // No tokens to check – must succeed (no assertion should fire)
        TestUtil.assertContainsIgnoreCase("any string");
    }

    @Test(expected = NullPointerException.class)
    public void assertContainsIgnoreCase_NullSource()
    {
        // source is null – the method will NPE when calling toLowerCase()
        TestUtil.assertContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void assertContainsIgnoreCase_NullToken()
    {
        // a null token causes a NPE inside the loop
        TestUtil.assertContainsIgnoreCase("some text", (String) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for checkContainsIgnoreCase                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void checkContainsIgnoreCase_HappyPath()
    {
        assertTrue(TestUtil.checkContainsIgnoreCase("Java Unit Testing", "java", "UNIT", "testing"));
    }

    @Test
    public void checkContainsIgnoreCase_MissingToken_ReturnsFalse()
    {
        assertFalse(TestUtil.checkContainsIgnoreCase("Java Unit Testing", "java", "missing"));
    }

    @Test
    public void checkContainsIgnoreCase_WrongOrder_ReturnsFalse()
    {
        // tokens exist but not in the supplied order
        assertFalse(TestUtil.checkContainsIgnoreCase("alpha beta gamma", "gamma", "beta"));
    }

    @Test
    public void checkContainsIgnoreCase_EmptyContainsArray_ReturnsTrue()
    {
        // By definition, an empty token list is always satisfied
        assertTrue(TestUtil.checkContainsIgnoreCase("whatever", new String[0]));
    }

    @Test(expected = NullPointerException.class)
    public void checkContainsIgnoreCase_NullSource()
    {
        TestUtil.checkContainsIgnoreCase(null, "anything");
    }

    @Test(expected = NullPointerException.class)
    public void checkContainsIgnoreCase_NullToken()
    {
        TestUtil.checkContainsIgnoreCase("some text", (String) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for fetchResource                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void fetchResource_ExistingFile_ReturnsContent()
    {
        // The file 'test.txt' must be placed in src/test/resources
        String expected = "Hello Resource";
        String actual = TestUtil.fetchResource("test.txt").trim(); // trim to ignore trailing newline
        assertEquals(expected, actual);
    }

    @Test(expected = RuntimeException.class)
    public void fetchResource_NonExistingFile_ThrowsRuntimeException()
    {
        // Resource does not exist – the method wraps the exception in RuntimeException
        TestUtil.fetchResource("does-not-exist-xyz.txt");
    }

    @Test(expected = RuntimeException.class)
    public void fetchResource_NullName_ThrowsRuntimeException()
    {
        // Passing null triggers a NPE inside getResource, which is wrapped
        TestUtil.fetchResource(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for isReleaseMode                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void isReleaseMode_DefaultIsFalse()
    {
        System.clearProperty(RELEASE_PROP);
        assertFalse("Default value should be false", TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_PropertyTrue()
    {
        System.setProperty(RELEASE_PROP, "true");
        assertTrue("Property set to true should return true", TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_PropertyFalse()
    {
        System.setProperty(RELEASE_PROP, "false");
        assertFalse("Property set to false should return false", TestUtil.isReleaseMode());
    }

    @Test
    public void isReleaseMode_PropertyInvalidValue()
    {
        // Any non‑true value is interpreted as false by Boolean.parseBoolean
        System.setProperty(RELEASE_PROP, "yes");
        assertFalse("Invalid boolean value should be interpreted as false", TestUtil.isReleaseMode());
    }
}
