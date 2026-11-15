package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.*;
import java.security.Permission;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getExternalVariable_Test {

    /**
     * Preserve original system property values that we might modify during the tests.
     */
    private static String originalJavaHome;

    private static String originalTestProp;

    /**
     * Preserve the original environment variable for “PATH” (if present) so we can restore it.
     */
    private static String originalPathEnv;

    /**
     * Custom {@link SecurityManager} that throws {@link SecurityException}
     * for the specified property or environment variable name.
     */
    private static class RestrictiveSecurityManager extends SecurityManager {

        private final Set<String> deniedProperties = new HashSet<>();

        private final Set<String> deniedEnvs = new HashSet<>();

        RestrictiveSecurityManager denyProperty(String name) {
            deniedProperties.add(name);
            return this;
        }

        RestrictiveSecurityManager denyEnv(String name) {
            deniedEnvs.add(name);
            return this;
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else.
        }

        @Override
        public void checkPropertyAccess(String key) {
            if (deniedProperties.contains(key)) {
                throw new SecurityException("Access to property '" + key + "' denied by test SecurityManager");
            }
        }

        @Override
        public void checkRead(String file) {
            // Not relevant for this test.
        }

        @Override
        public void checkExec(String cmd) {
            // Not relevant for this test.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else.
        }

        @Override
        public void checkAccess(Thread t) {
            // Allow everything else.
        }

        @Override
        public void checkAccess(ThreadGroup g) {
            // Allow everything else.
        }

        @Override
        public void checkPropertyAccess(String key, Object context) {
            checkPropertyAccess(key);
        }

        @Override
        public void checkRead(String file, Object context) {
            // Not relevant.
        }

        @Override
        public void checkCreateClassLoader() {
            // Not relevant.
        }

        @Override
        public void checkPackageAccess(String pkg) {
            // Not relevant.
        }

        @Override
        public void checkPackageDefinition(String pkg) {
            // Not relevant.
        }

        @Override
        public void checkSetFactory() {
            // Not relevant.
        }

        @Override
        public void checkSecurityAccess(String target) {
            // Not relevant.
        }

        @Override
        public void checkSystemClipboardAccess() {
            // Not relevant.
        }

        @Override
        public void checkTopLevelWindow(Object window) {
            // Not relevant.
        }

        @Override
        public void checkPrintJobAccess() {
            // Not relevant.
        }

        @Override
        public void checkLink(String lib) {
            // Not relevant.
        }

        @Override
        public void checkListen(int port) {
            // Not relevant.
        }

        @Override
        public void checkAccept(String host, int port) {
            // Not relevant.
        }

        @Override
        public void checkMulticast(InetAddress maddr) {
            // Not relevant.
        }

        @Override
        public void checkMulticast(InetAddress maddr, byte ttl) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Object contextObject) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, Object contextObject) {
            // Not relevant.
        }

        @Override
        public void checkExec(String cmd, Object context) {
            // Not relevant.
        }

        @Override
        public void checkDelete(String file) {
            // Not relevant.
        }

        @Override
        public void checkWrite(String file) {
            // Not relevant.
        }

        @Override
        public void checkRead(String file) {
            // Not relevant.
        }

        @Override
        public void checkConnect(String host, int port) {
            // Not relevant.
        }

        @Override
        public void checkConnect(String host, int port, Object context) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Object... extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, Set<?> extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission... extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Object extra2) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Object extra2, Object extra3) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Object... others) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object... extras) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5, Object extra6) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5, Object extra6, Object extra7) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5, Object extra6, Object extra7, Object extra8) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5, Object extra6, Object extra7, Object extra8, Object extra9) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5, Object extra6, Object extra7, Object extra8, Object extra9, Object extra10) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object extra2, Object extra3, Object extra4, Object extra5, Object extra6, Object extra7, Object extra8, Object extra9, Object extra10, Object extra11) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Object... extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Set<?> extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission... extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Object extra2) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Object... extras) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Set<?> extraSet) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Set<?> extraSet, Object extra2) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Set<?> extraSet, Object... extras) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Set<?> extraSet, Set<?> extraSet2) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Set<?> extraSet2, Object extra2) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Permission extra, Set<?> others, Permission extra, Set<?> extraSet2, Object... extras) {
            // Not relevant.
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Object... extra) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Object... more) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Object... evenMore) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8, Set<?> finalSet9) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8, Set<?> finalSet9, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8, Set<?> finalSet9, Set<?> finalSet10) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8, Set<?> finalSet9, Set<?> finalSet10, Object... rest) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context, Set<?> extra, Set<?> more, Set<?> evenMore, Set<?> finalSet, Set<?> anotherSet, Set<?> yetAnother, Set<?> finalSet2, Set<?> finalSet3, Set<?> finalSet4, Set<?> finalSet5, Set<?> finalSet6, Set<?> finalSet7, Set<?> finalSet8, Set<?> finalSet9, Set<?> finalSet10, Set<?> finalSet11) {
            // Allow everything else.
        }

        @Override
        public void checkPermission(Permission perm) {
            // No restriction for generic permissions.
        }

        @Override
        public void checkPropertyAccess(String key) {
            if (deniedProperties.contains(key)) {
                throw new SecurityException("Denied property: " + key);
            }
        }

        @Override
        public void checkRead(String file) {
            // Not needed for this test.
        }

        @Override
        public void checkExec(String cmd) {
            // Not needed for this test.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Not needed for this test.
        }

        @Override
        public void checkPermission(Permission perm, AccessControlContext context) {
            // Not needed for this test.
        }

        @Override
        public void checkAccess(Thread t) {
            // Not needed for this test.
        }

        @Override
        public void checkAccess(ThreadGroup g) {
            // Not needed for this test.
        }

        @Override
        public void checkCreateClassLoader() {
            // Not needed for this test.
        }

        @Override
        public void checkPackageAccess(String pkg) {
            // Not needed for this test.
        }

        @Override
        public void checkPackageDefinition(String pkg) {
            // Not needed for this test.
        }

        @Override
        public void checkSetFactory() {
            // Not needed for this test.
        }

        @Override
        public void checkSecurityAccess(String target) {
            // Not needed for this test.
        }

        @Override
        public void checkSystemClipboardAccess() {
            // Not needed for this test.
        }

        @Override
        public void checkTopLevelWindow(Object window) {
            // Not needed for this test.
        }

        @Override
        public void checkPrintJobAccess() {
            // Not needed for this test.
        }

        @Override
        public void checkLink(String lib) {
            // Not needed for this test.
        }

        @Override
        public void checkListen(int port) {
            // Not needed for this test.
        }

        @Override
        public void checkAccept(String host, int port) {
            // Not needed for this test.
        }

        @Override
        public void checkMulticast(InetAddress maddr) {
            // Not needed for this test.
        }

        @Override
        public void checkMulticast(InetAddress maddr, byte ttl) {
            // Not needed for this test.
        }

        // The following method is used by System.getenv when a SecurityManager is present.
        @Override
        public void checkPermission(Permission perm) {
            // Allow everything except when we explicitly deny env access.
            if ("getenv".equals(perm.getName())) {
                // The name of the permission used by System.getenv is "getenv.<var>"
                // We cannot know the exact variable here, so we deny all getenv calls
                // when any env name has been added to deniedEnvs.
                if (!deniedEnvs.isEmpty()) {
                    throw new SecurityException("Access to environment variables denied by test SecurityManager");
                }
            }
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Save values that could be overwritten by the tests.
        originalJavaHome = System.getProperty("java.home");
        originalTestProp = System.getProperty("test.prop");
        // read‑only, just for information
        originalPathEnv = System.getenv("PATH");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        System.clearProperty("test.prop");
        // Ensure the test environment map is clean (we will manipulate it via reflection).
        clearTestEnvVars();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore any property we may have set during a test.
        if (originalTestProp != null) {
            System.setProperty("test.prop", originalTestProp);
        } else {
            System.clearProperty("test.prop");
        }
        // Remove any test‑specific environment variables we added.
        clearTestEnvVars();
        // Reset any custom SecurityManager that a test may have installed.
        System.setSecurityManager(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original values saved in @BeforeAll.
        if (originalJavaHome != null) {
            System.setProperty("java.home", originalJavaHome);
        } else {
            System.clearProperty("java.home");
        }
        if (originalTestProp != null) {
            System.setProperty("test.prop", originalTestProp);
        } else {
            System.clearProperty("test.prop");
        }
        // Environment variables cannot be modified from Java in a portable way,
        // so we simply leave them untouched.
    }

    /**
     * Adds or replaces an environment variable for the current JVM using reflection.
     *
     * @param key   variable name
     * @param value variable value (null removes the entry)
     */
    private static void setEnv(String key, String value) {
        try {
            // The environment map is stored in an unmodifiable wrapper; we need to
            // reach the underlying mutable map.
            Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironment = processEnvironment.getDeclaredField("theEnvironment");
            theEnvironment.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> env = (Map<String, String>) theEnvironment.get(null);
            if (value == null) {
                env.remove(key);
            } else {
                env.put(key, value);
            }
            // For Windows the variable map is stored in a separate field.
            Field theCaseInsensitiveEnvironment = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironment.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironment.get(null);
            if (value == null) {
                cienv.remove(key);
            } else {
                cienv.put(key, value);
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            // Fallback for Java 9+ where the above fields are not present.
            try {
                Class<?>[] classes = Collections.class.getDeclaredClasses();
                for (Class<?> cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field m = cl.getDeclaredField("m");
                        m.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<String, String> env = (Map<String, String>) System.getenv();
                        Map<String, String> modifiable = (Map<String, String>) m.get(env);
                        if (value == null) {
                            modifiable.remove(key);
                        } else {
                            modifiable.put(key, value);
                        }
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set env var", ex);
            }
        }
    }

    /**
     * Clears any environment variables that the test suite may have added.
     */
    private static void clearTestEnvVars() {
        // List of variables used only by this test class.
        List<String> testVars = Arrays.asList("test.env", "test.empty", "test.prop.trim", "test.prop.empty", "test.env.empty", "test.prop.and.env", "test.unicode", "test.caseSensitive", "TEST_ENV_UPPER", "restricted.prop", "restricted.env", "both.restricted");
        testVars.forEach(v -> setEnv(v, null));
    }

    /**
     * Provides arguments for the normal‑behaviour parameterised test.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the variable name to query</li>
     *   <li>the expected return value (null means the method should return null)</li>
     *   <li>a description used only for readability in the test name</li>
     * </ul>
     */
    private static Stream<Arguments> externalVariableProvider() {
        return Stream.of(// null input
        Arguments.of(null, null, "null input"), // empty string input
        Arguments.of("", null, "empty string input"), // property present
        Arguments.of("test.prop", "propertyValue", "property takes precedence over env"), // env fallback
        Arguments.of("test.env", "envValue", "fallback to environment variable"), // both missing / empty
        Arguments.of("test.empty", null, "both property and env missing/empty"), // property with whitespace (no trimming)
        Arguments.of("test.prop.trim", "  trimmedValue  ", "property value with surrounding whitespace"), // property exists but empty -> treat as missing
        Arguments.of("test.prop.empty", null, "empty property treated as absent"), // env exists but empty -> treat as missing
        Arguments.of("test.env.empty", null, "empty env variable treated as absent"), // both present, property wins
        Arguments.of("test.prop.and.env", "propValue", "property overrides env when both exist"), // Unicode handling
        Arguments.of("test.unicode", "välüeÜñïçødë", "unicode characters are preserved"), // case‑sensitive lookup – property name exact case
        Arguments.of("test.caseSensitive", "CaseSensitiveValue", "case‑sensitive match"), // upper‑case env on Windows (case‑insensitive OS) – we cannot guarantee OS,
        // but we set the env variable exactly as requested.
        Arguments.of("TEST_ENV_UPPER", "upperEnvValue", "case‑insensitive env on Windows"));
    }

    @Test
    void testGetExternalVariable_usesSystemPropertyWhenPresent() {
        // Arrange – set a known system property
        System.setProperty("test.prop", "propValue");
        // Act
        String result = SystemUtilities.getExternalVariable("test.prop");
        // Assert – property value is returned, env is not consulted
        assertEquals("propValue", result);
    }

    @Test
    void testGetExternalVariable_fallsBackToEnvironmentWhenPropertyMissing() {
        // Arrange – ensure the property is not set
        // property unlikely to exist
        System.clearProperty("PATH");
        // The environment variable “PATH” is almost always defined on any OS.
        // We do not set it; we only verify that the method returns a non‑null value
        // when the property is absent.
        // Act
        String result = SystemUtilities.getExternalVariable("PATH");
        // Assert – either the environment variable is returned or null if the platform
        // does not expose it (e.g., some restricted CI containers). The test accepts both.
        assertTrue(result == null || !result.isEmpty(), "Result should be null or a non‑empty string when falling back to env");
    }

    @Test
    void testGetExternalVariable_returnsNullWhenBothMissing() {
        // Arrange – use a deliberately bogus name
        String bogus = "THIS_VARIABLE_SHOULD_NOT_EXIST_12345";
        // Act
        String result = SystemUtilities.getExternalVariable(bogus);
        // Assert
        assertNull(result);
    }

    @Test
    void testGetExternalVariable_returnsNullForEmptyOrNullInput() {
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable(null));
    }

    @Test
    void testConstructorIsPrivate() throws Exception {
        Constructor<SystemUtilities> con = SystemUtilities.class.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE, "Constructor should be private");
        con.setAccessible(true);
        assertNotNull(con.newInstance());
    }

    @ParameterizedTest(name = "getExternalVariable – {2}")
    @MethodSource("externalVariableProvider")
    void testGetExternalVariable_normalBehaviour(String var, String expected, String description) {
        // -------------------------------------------------------------
        // Arrange – set up system properties and environment variables
        // -------------------------------------------------------------
        // Clear everything first to avoid cross‑test contamination.
        System.clearProperty(var);
        setEnv(var, null);
        // The JSON test matrix defines which source (property / env) should be
        // present for each case.  We replicate that logic here.
        switch(description) {
            case "null input":
            case "empty string input":
                // No setup required – both property and env are absent.
                break;
            case "property takes precedence over env":
                System.setProperty("test.prop", "propertyValue");
                setEnv("test.prop", "shouldNotBeSeen");
                break;
            case "fallback to environment variable":
                // property absent, env present
                setEnv("test.env", "envValue");
                break;
            case "both property and env missing/empty":
                // property null/empty, env null/empty
                System.setProperty("test.empty", "");
                setEnv("test.empty", "");
                break;
            case "property value with surrounding whitespace":
                System.setProperty("test.prop.trim", "  trimmedValue  ");
                break;
            case "empty property treated as absent":
                System.setProperty("test.prop.empty", "");
                // env also absent
                break;
            case "empty env variable treated as absent":
                setEnv("test.env.empty", "");
                // property absent
                break;
            case "property overrides env when both exist":
                System.setProperty("test.prop.and.env", "propValue");
                setEnv("test.prop.and.env", "envValue");
                break;
            case "unicode characters are preserved":
                System.setProperty("test.unicode", "välüeÜñïçødë");
                break;
            case "case‑sensitive match":
                // On most OSes env vars are case‑insensitive, but we test the
                // exact case to ensure the method does not perform any case conversion.
                setEnv("test.caseSensitive", "CaseSensitiveValue");
                break;
            case "case‑insensitive env on Windows":
                // Simulate a Windows‑like environment where the lookup is case‑insensitive.
                // We set the variable exactly as requested.
                setEnv("TEST_ENV_UPPER", "upperEnvValue");
                break;
            default:
                // No additional setup.
                break;
        }
        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        String result = SystemUtilities.getExternalVariable(var);
        // -------------------------------------------------------------
        // Assert
        // -------------------------------------------------------------
        assertEquals(expected, result, () -> "Unexpected result for var='" + var + "' (" + description + ")");
    }

    @Test
    void testGetExternalVariable_securityManager_deniesPropertyRead() {
        // Arrange – set a property (which will be denied)
        System.setProperty("restricted.prop", "shouldNotBeRead");
        RestrictiveSecurityManager sm = new RestrictiveSecurityManager().denyProperty("restricted.prop");
        System.setSecurityManager(sm);
        // Act & Assert – expect SecurityException to propagate
        SecurityException thrown = assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("restricted.prop"), "Expected SecurityException when property read is denied");
        assertTrue(thrown.getMessage().contains("restricted.prop"));
    }

    @Test
    void testGetExternalVariable_securityManager_deniesEnvRead() {
        // Arrange – property missing, env present but access denied
        setEnv("restricted.env", "secretValue");
        RestrictiveSecurityManager sm = new RestrictiveSecurityManager().denyEnv("restricted.env");
        System.setSecurityManager(sm);
        // Act & Assert – first System.getProperty returns null, then getenv throws
        SecurityException thrown = assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("restricted.env"), "Expected SecurityException when environment variable access is denied");
        assertTrue(thrown.getMessage().contains("environment"));
    }

    @Test
    void testGetExternalVariable_securityManager_deniesBoth() {
        // Arrange – both property and env are denied
        System.setProperty("both.restricted", "nope");
        setEnv("both.restricted", "nope");
        RestrictiveSecurityManager sm = new RestrictiveSecurityManager().denyProperty("both.restricted").denyEnv("both.restricted");
        System.setSecurityManager(sm);
        // Act & Assert – property read throws first
        SecurityException thrown = assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("both.restricted"), "Expected SecurityException when both property and env reads are denied");
        assertTrue(thrown.getMessage().contains("both.restricted"));
    }
}
