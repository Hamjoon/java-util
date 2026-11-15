package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link SystemUtilities#getExternalVariable(String)}.
 *
 * <p>The tests cover all decision branches:
 * <ul>
 *   <li>System property is present and non‑empty → value is returned.</li>
 *   <li>System property is empty (null or &quot;&quot;) → fallback to environment variable.</li>
 *   <li>Environment variable is present and non‑empty → value is returned.</li>
 *   <li>Both property and environment variable are missing/empty → {@code null} is returned.</li>
 * </ul>
 *
 * <p>Because {@code System.getenv()} returns an unmodifiable map, the tests use reflection to
 * temporarily inject entries into the process environment. All modifications are restored
 * after each test to avoid side‑effects.</p>
 */
public class SystemUtilitiesTest {

    private static final String KEY = "SYSTEM_UTILITIES_TEST_KEY";

    // Original values are saved so we can restore them after the suite runs
    private String originalProperty;
    private Map<String, String> originalEnv;

    @Before
    public void setUp() throws Exception {
        // Preserve the original system property (may be null)
        originalProperty = System.getProperty(KEY);

        // Preserve a copy of the current environment map
        originalEnv = new HashMap<>(System.getenv());

        // Ensure a clean start for each test
        clearSystemProperty();
        clearEnvVariable();
    }

    @After
    public void tearDown() throws Exception {
        // Restore the original system property
        if (originalProperty != null) {
            System.setProperty(KEY, originalProperty);
        } else {
            System.clearProperty(KEY);
        }

        // Restore the original environment map
        setEnv(originalEnv);
    }

    /** Helper – clear the test system property */
    private void clearSystemProperty() {
        System.clearProperty(KEY);
    }

    /** Helper – remove the test key from the environment */
    private void clearEnvVariable() throws Exception {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.remove(KEY);
        setEnv(env);
    }

    /**
     * Injects the supplied map as the process environment using reflection.
     *
     * @param newenv the map that should become the new environment
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static void setEnv(Map<String, String> newenv) throws Exception {
        // For Java 9+ the internal class is "java.lang.ProcessEnvironment$Variable" etc.
        // The approach below works for both Oracle/OpenJDK implementations.

        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        // The environment map is stored in a static field called "theEnvironment"
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        env.clear();
        env.putAll(newenv);

        // On Windows there is a second map called "theCaseInsensitiveEnvironment"
        try {
            Field ciEnvField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            ciEnvField.setAccessible(true);
            Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
            ciEnv.clear();
            ciEnv.putAll(newenv);
        } catch (NoSuchFieldException ignored) {
            // Not Windows – nothing to do
        }

        // For Java 8 and earlier the map may be wrapped as an unmodifiable map;
        // replace the wrapper if present.
        Class<?>[] classes = Collections.class.getDeclaredClasses();
        for (Class<?> cl : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                Field m = cl.getDeclaredField("m");
                m.setAccessible(true);
                Object obj = System.getenv();
                if (cl.isInstance(obj)) {
                    Map<String, String> map = (Map<String, String>) m.get(obj);
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }

    /** Helper – set a single environment variable (overwrites any existing value). */
    private void setEnvVariable(String key, String value) throws Exception {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put(key, value);
        setEnv(env);
    }

    @Test
    public void testPropertyOverridesEnv() throws Exception {
        System.setProperty(KEY, "propertyValue");
        setEnvVariable(KEY, "envValue");

        String result = SystemUtilities.getExternalVariable(KEY);
        assertEquals("When a system property is present it must win over the environment variable.", "propertyValue", result);
    }

    @Test
    public void testEmptyPropertyFallsBackToEnv() throws Exception {
        System.setProperty(KEY, "");          // empty string → treated as “empty”
        setEnvVariable(KEY, "envOnly");

        String result = SystemUtilities.getExternalVariable(KEY);
        assertEquals("Empty property should cause a fallback to the environment variable.", "envOnly", result);
    }

    @Test
    public void testNullPropertyFallsBackToEnv() throws Exception {
        // No system property set (null)
        setEnvVariable(KEY, "onlyEnv");

        String result = SystemUtilities.getExternalVariable(KEY);
        assertEquals("When no property is set, the environment variable should be returned.", "onlyEnv", result);
    }

    @Test
    public void testNeitherPropertyNorEnvReturnsNull() throws Exception {
        // Ensure both are absent
        clearSystemProperty();
        clearEnvVariable();

        String result = SystemUtilities.getExternalVariable(KEY);
        assertNull("When neither property nor environment variable is present, null must be returned.", result);
    }

    @Test
    public void testEmptyEnvAndPropertyReturnsNull() throws Exception {
        System.setProperty(KEY, "");          // empty property
        setEnvVariable(KEY, "");             // empty environment variable

        String result = SystemUtilities.getExternalVariable(KEY);
        assertNull("Both property and environment variable empty → null.", result);
    }

    @Test
    public void testWhitespacePropertyIsNotEmpty() throws Exception {
        // Assuming StringUtilities.isEmpty treats only null or zero‑length as empty.
        System.setProperty(KEY, "   ");      // whitespace but length > 0
        setEnvVariable(KEY, "envValue");

        String result = SystemUtilities.getExternalVariable(KEY);
        assertEquals("Non‑empty (whitespace) property should be returned as‑is.", "   ", result);
    }
}
