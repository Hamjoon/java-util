package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SystemUtilitiesTest {

    // -----------------------------------------------------------------
    //  Helpers for manipulating the environment map (System.getenv) in
    //  a safe way for the duration of a test.
    // -----------------------------------------------------------------
    private Map<String, String> originalEnv;

    @Before
    public void captureOriginalEnv() throws Exception {
        // keep a copy of the current environment so we can restore it later
        originalEnv = new HashMap<>(System.getenv());
    }

    @After
    public void restoreEnvAndProperties() throws Exception {
        // restore all environment variables we may have changed
        setEnvMap(originalEnv);
        // clear any system properties we set during the tests
        System.clearProperty("test.prop");
        System.clearProperty("empty.prop");
        System.clearProperty("");
    }

    /**
     * Replace the entire environment map with the supplied map.
     * This works on the standard Oracle/OpenJDK implementation where
     * System.getenv() returns a Collections$UnmodifiableMap that wraps a
     * mutable map stored in the private field “m”.
     */
    @SuppressWarnings("unchecked")
    private void setEnvMap(Map<String, String> newEnv) throws Exception {
        Map<String, String> env = System.getenv();
        Class<?> envClass = env.getClass();

        // The unmodifiable map holds the real map in a private field called "m"
        Field field = envClass.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> mutableEnv = (Map<String, String>) field.get(env);
        mutableEnv.clear();
        mutableEnv.putAll(newEnv);
    }

    /**
     * Set a single environment variable for the current JVM.
     */
    private void setEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Class<?> envClass = env.getClass();
        Field field = envClass.getDeclaredField("m");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> mutableEnv = (Map<String, String>) field.get(env);
        mutableEnv.put(key, value);
    }

    // -----------------------------------------------------------------
    //  Basic functional tests
    // -----------------------------------------------------------------

    @Test
    public void testGetExternalVariable_PropertyOverridesEnv() {
        System.setProperty("test.prop", "propValue");
        // also set an env var with the same key – it should be ignored
        try {
            setEnv("test.prop", "envValue");
        } catch (Exception e) {
            // if we cannot set env (unlikely on the test platform) we just skip it
        }

        String result = SystemUtilities.getExternalVariable("test.prop");
        assertEquals("Property value should win over environment variable", "propValue", result);
    }

    @Test
    public void testGetExternalVariable_EmptyPropertyUsesEnv() {
        // Property exists but is empty – should fall back to env var
        System.setProperty("empty.prop", "");
        try {
            setEnv("empty.prop", "envOnly");
        } catch (Exception e) {
            fail("Failed to set environment variable for test");
        }

        String result = SystemUtilities.getExternalVariable("empty.prop");
        assertEquals("Empty property should cause fallback to env var", "envOnly", result);
    }

    @Test
    public void testGetExternalVariable_NoVariableReturnsNull() {
        // Ensure neither a system property nor an env var exists
        System.clearProperty("nonexistent");
        // remove possible env entry if present
        try {
            setEnvMap(originalEnv); // reset env to original (which likely does not have the key)
        } catch (Exception e) {
            fail("Failed to reset environment map");
        }

        String result = SystemUtilities.getExternalVariable("nonexistent");
        assertNull("When neither property nor env var exist, result must be null", result);
    }

    // -----------------------------------------------------------------
    //  Edge‑case and exception tests
    // -----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testGetExternalVariable_NullArgumentThrowsNPE() {
        // System.getProperty(null) throws NPE – our method should propagate it
        SystemUtilities.getExternalVariable(null);
    }

    @Test
    public void testGetExternalVariable_EmptyStringKey() {
        // An empty string key is legal for System.getProperty / getenv
        System.setProperty("", "emptyKeyProp");
        try {
            setEnv("", "emptyKeyEnv");
        } catch (Exception e) {
            // ignore – not all JVMs allow empty‑key env vars
        }

        String result = SystemUtilities.getExternalVariable("");
        // Property should be returned (it is non‑empty)
        assertEquals("Empty string key should return the property value", "emptyKeyProp", result);
    }

    @Test
    public void testGetExternalVariable_PropertyEmptyAndEnvEmptyReturnsNull() {
        System.setProperty("both.empty", "");
        try {
            setEnv("both.empty", "");
        } catch (Exception e) {
            // ignore – some platforms may not allow empty env values
        }

        String result = SystemUtilities.getExternalVariable("both.empty");
        assertNull("Both property and env var empty => null", result);
    }

    @Test
    public void testGetExternalVariable_PropertyNullAndEnvPresent() {
        // Simulate a property that is not set (null) and an env var present
        System.clearProperty("only.env");
        try {
            setEnv("only.env", "envOnlyValue");
        } catch (Exception e) {
            fail("Failed to set environment variable for test");
        }

        String result = SystemUtilities.getExternalVariable("only.env");
        assertEquals("When property is null, env var should be returned", "envOnlyValue", result);
    }
}
