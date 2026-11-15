package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getExternalVariable_Test {

    /**
     * Keys used in the tests – they are unlikely to clash with real system properties.
     */
    private static final String TEST_PROP_KEY = "systemutilities.test.prop";

    // most OSes provide this env variable
    private static final String TEST_ENV_KEY = "PATH";

    /**
     * Holds original system‑property values so they can be restored after the suite runs.
     */
    private static final Map<String, String> ORIGINAL_PROPERTIES = new HashMap<>();

    /**
     * Holds original environment variable values for the keys we tamper with.
     */
    private static final Map<String, String> ORIGINAL_ENV = new HashMap<>();

    /**
     * Original SecurityManager (if any) – restored after security‑related tests.
     */
    private static SecurityManager originalSecurityManager;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve any existing value for the test property key.
        ORIGINAL_PROPERTIES.put(TEST_PROP_KEY, System.getProperty(TEST_PROP_KEY));
        originalSecurityManager = System.getSecurityManager();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        System.clearProperty(TEST_PROP_KEY);
        clearAllTestEnvVars();
    }

    @AfterEach
    void teardownAfterEach() {
        // Remove the test property after each test to avoid cross‑test contamination.
        System.clearProperty(TEST_PROP_KEY);
        clearAllTestEnvVars();
        // Reset any custom SecurityManager that a test may have installed.
        System.setSecurityManager(originalSecurityManager);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original value (if any) for the test property key.
        String original = ORIGINAL_PROPERTIES.get(TEST_PROP_KEY);
        if (original != null) {
            System.setProperty(TEST_PROP_KEY, original);
        } else {
            System.clearProperty(TEST_PROP_KEY);
        }
    }

    /**
     * Sets (or overrides) an environment variable for the current JVM.
     *
     * @param key   variable name
     * @param value variable value (null removes the entry)
     */
    private static void setEnv(String key, String value) throws Exception {
        // Store original value for later restoration.
        if (!ORIGINAL_ENV.containsKey(key)) {
            ORIGINAL_ENV.put(key, System.getenv(key));
        }
        // The environment map is stored in the ProcessEnvironment class (JDK internal).
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        if (value == null) {
            env.remove(key);
        } else {
            env.put(key, value);
        }
        // For Windows the case‑insensitive map must also be updated.
        try {
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            if (value == null) {
                cienv.remove(key);
            } else {
                cienv.put(key, value);
            }
        } catch (NoSuchFieldException ignored) {
            // Not Windows – nothing more to do.
        }
    }

    /**
     * Clears all environment variables that were touched by the tests,
     * restoring them to their original values.
     */
    private static void clearAllTestEnvVars() throws Exception {
        for (String key : new HashSet<>(ORIGINAL_ENV.keySet())) {
            String original = ORIGINAL_ENV.get(key);
            setEnv(key, original);
        }
        ORIGINAL_ENV.clear();
    }

    /**
     * Supplies arguments for the "Null or Empty Variable Name" group.
     */
    private static Stream<Arguments> nullOrEmptyProvider() {
        return Stream.of(Arguments.of(null, null), Arguments.of("", null), Arguments.of("   ", null));
    }

    /**
     * Supplies arguments for the "System Property Exists – Takes Precedence" group.
     */
    private static Stream<Arguments> systemPropertyPrecedenceProvider() throws Exception {
        // Prepare environment variables needed for the cases.
        setEnv("TEST_PROP_EMPTY", "envValue");
        setEnv("TEST_PROP_NULL", "envValue");
        return Stream.of(// property present, non‑empty → property wins
        Arguments.of("TEST_PROP", "systemValue", "systemValue"), // property empty → fallback to env
        Arguments.of("TEST_PROP_EMPTY", "", "envValue"), // property missing (null) → fallback to env
        Arguments.of("TEST_PROP_NULL", null, "envValue"));
    }

    /**
     * Supplies arguments for the "Fallback to Environment Variable" group.
     */
    private static Stream<Arguments> fallbackToEnvProvider() throws Exception {
        setEnv("ONLY_ENV", "onlyEnvValue");
        // empty env var
        setEnv("ENV_EMPTY", "");
        return Stream.of(Arguments.of("ONLY_ENV", "onlyEnvValue"), Arguments.of("ENV_EMPTY", null));
    }

    /**
     * Supplies arguments for the "Both Sources Empty" group.
     */
    private static Stream<Arguments> bothEmptyProvider() throws Exception {
        setEnv("EMPTY_BOTH", "");
        return Stream.of(Arguments.of("EMPTY_BOTH", null));
    }

    /**
     * Supplies arguments for the "Property Precedence with Different Values" group.
     */
    private static Stream<Arguments> propertyPrecedenceProvider() throws Exception {
        setEnv("DUPLICATE_VAR", "envValue");
        return Stream.of(Arguments.of("DUPLICATE_VAR", "propValue"));
    }

    /**
     * Supplies arguments for the "Environment Variable Empty String" group.
     */
    private static Stream<Arguments> envOnlyEmptyProvider() throws Exception {
        setEnv("ENV_ONLY_EMPTY", "");
        return Stream.of(Arguments.of("ENV_ONLY_EMPTY", null));
    }

    /**
     * Supplies arguments for the "Property Exists Non‑Empty, Environment Missing" group.
     */
    private static Stream<Arguments> propOnlyProvider() {
        return Stream.of(Arguments.of("PROP_ONLY", "onlyProp"));
    }

    @Test
    void testReturnsSystemPropertyWhenPresent() {
        String expected = "propertyValue";
        System.setProperty(TEST_PROP_KEY, expected);
        String actual = SystemUtilities.getExternalVariable(TEST_PROP_KEY);
        assertNotNull(actual, "The method should return a non‑null value when the property exists.");
        assertEquals(expected, actual, "The returned value must match the system property value.");
    }

    @Test
    void testFallsBackToEnvironmentVariableWhenPropertyMissing() {
        // Ensure the property is not set.
        System.clearProperty(TEST_ENV_KEY);
        // The environment variable "PATH" (or "Path" on Windows) is almost always present.
        String envValue = System.getenv(TEST_ENV_KEY);
        if (envValue == null) {
            // On Windows the variable name is case‑insensitive; try the alternative spelling.
            envValue = System.getenv("Path");
        }
        assertNotNull(envValue, "The test environment must provide a PATH variable for this test.");
        String actual = SystemUtilities.getExternalVariable(TEST_ENV_KEY);
        assertNotNull(actual, "The method should return the environment variable value when the system property is absent.");
        assertEquals(envValue, actual, "The returned value must match the environment variable value.");
    }

    @Test
    void testReturnsNullForNonExistentKey() {
        String nonExistentKey = "systemutilities.non.existent.key";
        assertNull(SystemUtilities.getExternalVariable(nonExistentKey), "A non‑existent key should result in null.");
    }

    @Test
    void testReturnsNullForEmptyOrNullInput() {
        assertNull(SystemUtilities.getExternalVariable(""), "An empty string should yield null.");
        assertNull(SystemUtilities.getExternalVariable(null), "A null argument should yield null.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}'' → expected=null")
    @MethodSource("nullOrEmptyProvider")
    void testNullOrEmptyVariableName(String var, String expected) {
        assertNull(SystemUtilities.getExternalVariable(var), "Method should return null for empty or whitespace‑only variable names.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}''")
    @MethodSource("systemPropertyPrecedenceProvider")
    void testSystemPropertyPrecedence(String var, String propValue, String expected) throws Exception {
        // Set the system property according to the test case.
        if (propValue != null) {
            System.setProperty(var, propValue);
        } else {
            System.clearProperty(var);
        }
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(expected, actual, "When property is empty or missing the method must fall back to the environment variable.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}'' → expected=''{1}''")
    @MethodSource("fallbackToEnvProvider")
    void testFallbackToEnvironmentVariable(String var, String expected) throws Exception {
        // Ensure no system property interferes.
        System.clearProperty(var);
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(expected, actual, "Method should return the environment variable value when the property is absent, or null if the env var is empty.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}'' → expected=null")
    @MethodSource("bothEmptyProvider")
    void testBothSourcesEmpty(String var, String expected) throws Exception {
        // empty property
        System.setProperty(var, "");
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(expected, actual, "When both property and environment variable are empty the method must return null.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}'' → expected='propValue'")
    @MethodSource("propertyPrecedenceProvider")
    void testPropertyPrecedenceOverEnv(String var, String propValue) throws Exception {
        System.setProperty(var, propValue);
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(propValue, actual, "System property must take precedence over an environment variable with the same name.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}'' → expected=null")
    @MethodSource("envOnlyEmptyProvider")
    void testEnvOnlyEmptyString(String var, String expected) throws Exception {
        System.clearProperty(var);
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(expected, actual, "An empty environment variable should be treated as missing and result in null.");
    }

    @ParameterizedTest(name = "[{index}] var=''{0}'' → expected=''{1}''")
    @MethodSource("propOnlyProvider")
    void testPropertyExistsNoEnv(String var, String propValue) {
        System.setProperty(var, propValue);
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(propValue, actual, "When only the system property exists, its value should be returned.");
    }

    @Test
    void testVariableNameWithLeadingTrailingWhitespace() {
        String var = "  TRIMMED_VAR  ";
        // No property or env with the exact (spaced) key.
        System.clearProperty(var);
        assertNull(SystemUtilities.getExternalVariable(var), "Whitespace in the key prevents a match; method should return null.");
    }

    @Test
    void testVariableNameWithNonAsciiCharacters() throws Exception {
        String var = "变量";
        String expected = "valueUnicode";
        setEnv(var, expected);
        System.clearProperty(var);
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals(expected, actual, "Method must correctly handle Unicode variable names.");
    }

    @Test
    void testVariableNameVeryLong() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) sb.append('A');
        String var = sb.toString();
        assertNull(SystemUtilities.getExternalVariable(var), "A very long, non‑existent key should safely return null.");
    }

    @Test
    void testPropertyValueLiteralNullString() {
        String var = "LITERAL_NULL";
        // literal string, not a null reference
        System.setProperty(var, "null");
        String actual = SystemUtilities.getExternalVariable(var);
        assertEquals("null", actual, "The literal string \"null\" is a non‑empty value and must be returned as‑is.");
    }

    @Test
    void testSecurityExceptionOnSystemPropertyAccess() {
        // Install a SecurityManager that forbids reading the specific property.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPropertyAccess(String key) {
                if ("SECURE_PROP".equals(key)) {
                    throw new SecurityException("Access denied to property: " + key);
                }
            }
        });
        // Ensure the property is not set (or could be set – it doesn't matter; access will be denied).
        System.clearProperty("SECURE_PROP");
        assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("SECURE_PROP"), "Method should propagate SecurityException from System.getProperty.");
    }

    @Test
    void testSecurityExceptionOnEnvironmentVariableAccess() {
        // Install a SecurityManager that forbids environment variable access.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkExec(String cmd) {
                // Allow.
            }

            @Override
            public void checkRead(String file) {
                // Allow.
            }

            @Override
            public void checkPropertyAccess(String key) {
                // Allow property reads.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18, java.security.AccessControlContext context19) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18, java.security.AccessControlContext context19, java.security.AccessControlContext context20) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18, java.security.AccessControlContext context19, java.security.AccessControlContext context20, java.security.AccessControlContext context21) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18, java.security.AccessControlContext context19, java.security.AccessControlContext context20, java.security.AccessControlContext context21, java.security.AccessControlContext context22) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18, java.security.AccessControlContext context19, java.security.AccessControlContext context20, java.security.AccessControlContext context21, java.security.AccessControlContext context22, java.security.AccessControlContext context23) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12, java.security.AccessControlContext context13, java.security.AccessControlContext context14, java.security.AccessControlContext context15, java.security.AccessControlContext context16, java.security.AccessControlContext context17, java.security.AccessControlContext context18, java.security.AccessControlContext context19, java.security.AccessControlContext context20, java.security.AccessControlContext context21, java.security.AccessControlContext context22, java.security.AccessControlContext context23, java.security.AccessControlContext context24) {
                // Allow.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4, java.security.AccessControlContext context5, java.security.AccessControlContext context6, java.security.AccessControlContext context7, java.security.AccessControlContext context8, java.security.AccessControlContext context9, java.security.AccessControlContext context10, java.security.AccessControlContext context11, java.security.AccessControlContext context12) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context, java.security.AccessControlContext context2, java.security.AccessControlContext context3, java.security.AccessControlContext context4) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, java.security.AccessControlContext context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            // Deny env var access for the specific key.
            @Override
            public void checkPermission(Permission perm) {
                if ("getenv.secure".equals(perm.getName())) {
                    throw new SecurityException("Access to environment variables denied");
                }
            }
        });
        // Ensure property missing so method reaches env access.
        System.clearProperty("SECURE_ENV");
        // The env variable may exist; we don't need to set it.
        assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("SECURE_ENV"), "Method should propagate SecurityException from System.getenv.");
    }
}
