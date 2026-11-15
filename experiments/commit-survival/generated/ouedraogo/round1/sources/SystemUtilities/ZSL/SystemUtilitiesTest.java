/****************************************************************************************
 * Unit tests for {@link com.cedarsoftware.util.SystemUtilities}
 *
 * These tests exercise all logical branches of the single public method
 * {@code getExternalVariable(String)}.  The method prefers a system property, falls
 * back to an environment variable, and returns {@code null} when neither is set.
 *
 * The tests manipulate {@link System#getProperties()} directly (which is safe in a
 * unit‑test environment) and also modify the process environment map via reflection.
 * The original state is saved and restored in @Before/@After methods so the test
 * suite does not affect the JVM that runs the tests.
 *
 * JUnit 4 is used as requested.
 ****************************************************************************************/
package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SystemUtilitiesTest
{
    /* -------------------------------------------------------------------------
     *  State that is saved before each test and restored afterwards.
     * ------------------------------------------------------------------------- */
    private Map<String, String> originalEnv;
    private String originalPropertyValue;

    private static final String TEST_KEY = "SYSTEM_UTILS_TEST_KEY";

    /* -------------------------------------------------------------------------
     *  Helper methods for fiddling with the (normally) immutable environment map.
     * ------------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<String, String> getModifiableEnv() throws Exception
    {
        // In Oracle/OpenJDK the environment map is stored in a private static field.
        // The implementation differs between Java versions, but the two most common
        // ones (Java 8 and Java 9+) expose a field named "theUnmodifiableEnvironment"
        // or a "m" field inside an inner class.  The code below works for both.
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");

        // Java 8: there is a static field "theUnmodifiableEnvironment".
        try
        {
            Field unmodifiableEnvField = processEnvironmentClass.getDeclaredField("theUnmodifiableEnvironment");
            unmodifiableEnvField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) unmodifiableEnvField.get(null);
            // The map is wrapped with Collections.unmodifiableMap, so we need the underlying map.
            Field mField = env.getClass().getDeclaredField("m");
            mField.setAccessible(true);
            return (Map<String, String>) mField.get(env);
        }
        catch (NoSuchFieldException ignored)
        {
            // Java 9+ stores the environment in an unmodifiable map inside the
            // ProcessEnvironment$Variable class.
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            return (Map<String, String>) theEnvironmentField.get(null);
        }
    }

    private static void setEnv(String key, String value) throws Exception
    {
        Map<String, String> modifiableEnv = getModifiableEnv();
        if (value == null)
        {
            modifiableEnv.remove(key);
        }
        else
        {
            modifiableEnv.put(key, value);
        }
    }

    /* -------------------------------------------------------------------------
     *  Save/restore original state
     * ------------------------------------------------------------------------- */
    @Before
    public void setUp() throws Exception
    {
        // Save original system property (might be null)
        originalPropertyValue = System.getProperty(TEST_KEY);

        // Save a copy of the current environment
        originalEnv = new HashMap<>(System.getenv());

        // Ensure a clean slate for the test key
        System.clearProperty(TEST_KEY);
        setEnv(TEST_KEY, null);
    }

    @After
    public void tearDown() throws Exception
    {
        // Restore original system property
        if (originalPropertyValue == null)
        {
            System.clearProperty(TEST_KEY);
        }
        else
        {
            System.setProperty(TEST_KEY, originalPropertyValue);
        }

        // Restore original environment
        // First clear any test changes
        setEnv(TEST_KEY, null);
        // Then put back everything that existed before the test run
        for (Map.Entry<String, String> entry : originalEnv.entrySet())
        {
            setEnv(entry.getKey(), entry.getValue());
        }
    }

    /* -------------------------------------------------------------------------
     *  Test cases
     * ------------------------------------------------------------------------- */

    /**
     * When a non‑empty system property is present, the method must return that value
     * and never consult the environment.
     */
    @Test
    public void testReturnsSystemPropertyWhenPresent()
    {
        System.setProperty(TEST_KEY, "propertyValue");
        // Even if an environment variable exists, it should be ignored.
        try
        {
            setEnv(TEST_KEY, "envValue");
        }
        catch (Exception e)
        {
            fail("Failed to set env var for test: " + e);
        }

        String result = SystemUtilities.getExternalVariable(TEST_KEY);
        assertEquals("propertyValue", result);
    }

    /**
     * An empty string system property should be treated as “not set” and cause a
     * fallback to the environment variable.
     */
    @Test
    public void testEmptySystemPropertyFallsBackToEnv()
    {
        System.setProperty(TEST_KEY, ""); // empty string
        try
        {
            setEnv(TEST_KEY, "envOnlyValue");
        }
        catch (Exception e)
        {
            fail("Failed to set env var for test: " + e);
        }

        String result = SystemUtilities.getExternalVariable(TEST_KEY);
        assertEquals("envOnlyValue", result);
    }

    /**
     * When the system property is missing and an environment variable is defined,
     * the method should return the environment variable.
     */
    @Test
    public void testReturnsEnvVariableWhenPropertyMissing()
    {
        System.clearProperty(TEST_KEY);
        try
        {
            setEnv(TEST_KEY, "onlyEnvValue");
        }
        catch (Exception e)
        {
            fail("Failed to set env var for test: " + e);
        }

        String result = SystemUtilities.getExternalVariable(TEST_KEY);
        assertEquals("onlyEnvValue", result);
    }

    /**
     * If neither a system property nor an environment variable is defined,
     * the method must return {@code null}.
     */
    @Test
    public void testReturnsNullWhenNeitherDefined()
    {
        System.clearProperty(TEST_KEY);
        try
        {
            setEnv(TEST_KEY, null);
        }
        catch (Exception e)
        {
            fail("Failed to clear env var for test: " + e);
        }

        String result = SystemUtilities.getExternalVariable(TEST_KEY);
        assertNull(result);
    }

    /**
     * When both a system property and an environment variable are present,
     * the system property takes precedence.
     */
    @Test
    public void testPropertyPrecedenceOverEnv()
    {
        System.setProperty(TEST_KEY, "propValue");
        try
        {
            setEnv(TEST_KEY, "envValue");
        }
        catch (Exception e)
        {
            fail("Failed to set env var for test: " + e);
        }

        String result = SystemUtilities.getExternalVariable(TEST_KEY);
        assertEquals("propValue", result);
    }

    /**
     * Verify that the method treats a {@code null} system property the same as an
     * empty string – it should fall back to the environment variable.
     */
    @Test
    public void testNullSystemPropertyFallsBackToEnv()
    {
        System.setProperty(TEST_KEY, null);
        try
        {
            setEnv(TEST_KEY, "envWhenPropNull");
        }
        catch (Exception e)
        {
            fail("Failed to set env var for test: " + e);
        }

        String result = SystemUtilities.getExternalVariable(TEST_KEY);
        assertEquals("envWhenPropNull", result);
    }
}
