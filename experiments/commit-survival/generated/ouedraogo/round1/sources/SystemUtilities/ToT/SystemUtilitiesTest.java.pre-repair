package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SystemUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage – a system property is defined and should be returned.</li>
 *   <li>Fallback to an environment variable when the property is missing or empty.</li>
 *   <li>When neither source provides a value the method must return {@code null}.</li>
 *   <li>Precedence – a defined property overrides an existing environment variable.</li>
 *   <li>Error handling – passing {@code null} as the variable name must result in a
 *       {@link NullPointerException} (the JDK throws this for {@code System.getProperty(null)}).</li>
 * </ul>
 *
 * <p>Because {@code System.getenv(..)} cannot be modified at runtime,
 * the tests that rely on an environment variable use a well‑known variable
 * that is almost always present on any OS (“PATH”).  The test suite also
 * guards against the unlikely case that the variable is absent by using
 * {@link org.junit.Assume#assumeTrue}.</p>
 *
 * <p>All modifications to system properties are saved and restored in {@code @Before}
 * / {@code @After} methods so the test suite does not affect the JVM state outside
 * of its own execution.</p>
 */
public class SystemUtilitiesTest
{
    /** Name of a system property that we control only inside the test suite. */
    private static final String TEST_PROP = "com.cedarsoftware.util.testProp";

    /** Name of an environment variable that is almost guaranteed to exist on every platform. */
    private static final String ENV_VAR = "PATH";

    /** Original value of {@link #TEST_PROP} before the test run (may be {@code null}). */
    private String originalPropValue;

    /** Original value of {@link #ENV_VAR} (captured only for documentation; we never modify it). */
    private String originalEnvValue;

    @Before
    public void setUp()
    {
        // Preserve the original system property (if any) so we can restore it later.
        originalPropValue = System.getProperty(TEST_PROP);

        // Capture the environment variable value for the assumption in the tests.
        originalEnvValue = System.getenv(ENV_VAR);
    }

    @After
    public void tearDown()
    {
        // Restore the original system property state.
        if (originalPropValue == null)
        {
            System.clearProperty(TEST_PROP);
        }
        else
        {
            System.setProperty(TEST_PROP, originalPropValue);
        }
    }

    /**
     * Test that a non‑empty system property value is returned and that the
     * environment variable is ignored even if it is present.
     */
    @Test
    public void testPropertyTakesPrecedenceOverEnv()
    {
        String expected = "propertyValue";
        System.setProperty(TEST_PROP, expected);

        // Even though PATH is set, the method must return the property value.
        String actual = SystemUtilities.getExternalVariable(TEST_PROP);
        assertEquals("System property should be returned when present", expected, actual);
    }

    /**
     * Test that when the system property is missing, the method falls back to the
     * environment variable value.
     */
    @Test
    public void testFallbackToEnvironmentVariable()
    {
        // Ensure the property is not defined.
        System.clearProperty(TEST_PROP);

        // Guard against the unlikely situation where PATH is not defined.
        org.junit.Assume.assumeTrue("Environment variable PATH must be defined for this test",
                originalEnvValue != null && !originalEnvValue.isEmpty());

        String actual = SystemUtilities.getExternalVariable(ENV_VAR);
        assertEquals("When property is absent, the environment variable value should be returned",
                originalEnvValue, actual);
    }

    /**
     * Test that an empty system property causes the method to fall back to the
     * environment variable.
     */
    @Test
    public void testEmptyPropertyFallsBackToEnv()
    {
        // Set the property to an empty string.
        System.setProperty(TEST_PROP, "");

        // Guard against missing PATH.
        org.junit.Assume.assumeTrue("Environment variable PATH must be defined for this test",
                originalEnvValue != null && !originalEnvValue.isEmpty());

        String actual = SystemUtilities.getExternalVariable(TEST_PROP);
        assertEquals("Empty property should be ignored and env variable used",
                originalEnvValue, actual);
    }

    /**
     * Test that when neither a system property nor an environment variable is
     * defined for the given key, the method returns {@code null}.
     */
    @Test
    public void testReturnsNullWhenNotDefined()
    {
        // Choose a key that is very unlikely to exist anywhere.
        String unknownKey = "com.cedarsoftware.util.thisKeyDoesNotExist";

        // Ensure no system property exists.
        System.clearProperty(unknownKey);

        String result = SystemUtilities.getExternalVariable(unknownKey);
        assertNull("Method should return null when no property or env variable is found", result);
    }

    /**
     * Verify that passing {@code null} as the variable name results in a
     * {@link NullPointerException}.  This mirrors the behaviour of
     * {@code System.getProperty(null)} which throws NPE.
     */
    @Test(expected = NullPointerException.class)
    public void testNullArgumentThrowsException()
    {
        SystemUtilities.getExternalVariable(null);
    }
}
