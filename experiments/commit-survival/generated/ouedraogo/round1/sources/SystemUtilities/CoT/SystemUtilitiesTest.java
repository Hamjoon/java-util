package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SystemUtilitiesTest {

    // A key that is very unlikely to exist in either system properties or environment variables
    private static final String NON_EXISTENT_KEY = "___NON_EXISTENT_KEY_12345___";

    // A key that is guaranteed to exist as an environment variable on most OSes
    private static final String COMMON_ENV_KEY = "PATH";

    // Backup of any original property values so we can restore them after the tests
    private String originalPropertyValue;

    @Before
    public void setUp() {
        // Preserve any existing property for the test keys
        originalPropertyValue = System.getProperty(NON_EXISTENT_KEY);
        System.clearProperty(NON_EXISTENT_KEY);
        System.clearProperty(COMMON_ENV_KEY);
    }

    @After
    public void tearDown() {
        // Restore original property values
        if (originalPropertyValue != null) {
            System.setProperty(NON_EXISTENT_KEY, originalPropertyValue);
        } else {
            System.clearProperty(NON_EXISTENT_KEY);
        }
        System.clearProperty(COMMON_ENV_KEY);
    }

    /*** Basic functionality ***/

    @Test
    public void testReturnsSystemPropertyWhenPresent() {
        System.setProperty(NON_EXISTENT_KEY, "propValue");
        String result = SystemUtilities.getExternalVariable(NON_EXISTENT_KEY);
        assertEquals("propValue", result);
    }

    @Test
    public void testFallsBackToEnvWhenPropertyMissing() {
        // Ensure the property is not set
        System.clearProperty(COMMON_ENV_KEY);
        // The environment variable PATH should be present on virtually all machines
        String envValue = System.getenv(COMMON_ENV_KEY);
        assertNotNull("Environment variable PATH must exist for this test", envValue);

        String result = SystemUtilities.getExternalVariable(COMMON_ENV_KEY);
        assertEquals(envValue, result);
    }

    @Test
    public void testReturnsNullWhenNeitherPropertyNorEnvExists() {
        String result = SystemUtilities.getExternalVariable(NON_EXISTENT_KEY);
        assertNull(result);
    }

    /*** Edge‑case tests ***/

    @Test(expected = NullPointerException.class)
    public void testNullKeyThrowsNullPointerException() {
        SystemUtilities.getExternalVariable(null);
    }

    @Test
    public void testEmptyStringPropertyFallsBackToEnv() {
        // Set an empty string as the property value
        System.setProperty(COMMON_ENV_KEY, "");
        // Ensure the environment variable exists
        String envValue = System.getenv(COMMON_ENV_KEY);
        assertNotNull("Environment variable PATH must exist for this test", envValue);

        String result = SystemUtilities.getExternalVariable(COMMON_ENV_KEY);
        // Because the property is empty, the method should fall back to the env variable
        assertEquals(envValue, result);
    }

    @Test
    public void testPropertyOverridesEnvEvenIfEnvIsPresent() {
        // Set both a property and rely on the existing environment variable
        System.setProperty(COMMON_ENV_KEY, "myPropValue");
        String envValue = System.getenv(COMMON_ENV_KEY);
        assertNotNull("Environment variable PATH must exist for this test", envValue);
        // The property value should win
        String result = SystemUtilities.getExternalVariable(COMMON_ENV_KEY);
        assertEquals("myPropValue", result);
    }

    @Test
    public void testWhitespacePropertyIsReturnedAsIs() {
        // Whitespace is not considered empty by StringUtilities.isEmpty (assumption)
        System.setProperty(NON_EXISTENT_KEY, "   ");
        String result = SystemUtilities.getExternalVariable(NON_EXISTENT_KEY);
        assertEquals("   ", result);
    }
}
