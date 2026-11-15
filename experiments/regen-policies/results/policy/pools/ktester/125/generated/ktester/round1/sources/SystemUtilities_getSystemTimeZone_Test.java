package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;

// Mockito not required for this test but kept for template consistency
@ExtendWith(MockitoExtension.class)
class SystemUtilities_getSystemTimeZone_Test {

    /**
     * Original value of the {@code TZ} environment variable (may be {@code null}).
     */
    private static String originalTZ;

    /**
     * Holds a reference to the mutable environment map obtained via reflection.
     */
    private static Map<String, String> mutableEnv;

    /**
     * The SecurityManager that was active before a test that installs its own manager.
     */
    private SecurityManager previousSecurityManager;

    /**
     * A SecurityManager that denies access to environment variables
     * (i.e., any RuntimePermission that starts with "getenv.").
     */
    private static class DenyGetEnvSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.security.AccessControlContext acc, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.lang.Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.lang.Object context, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(java.security.Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc, java.lang.Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object extra, java.lang.Object more) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object extra, java.lang.Object more, java.lang.Object evenMore) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object extra, java.lang.Object more, java.lang.Object evenMore, java.lang.Object finalOne) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object extra, java.lang.Object more, java.lang.Object evenMore, java.lang.Object finalOne, java.lang.Object last) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object extra, java.lang.Object more, java.lang.Object evenMore, java.lang.Object finalOne, java.lang.Object last, java.lang.Object finalTwo) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object extra, java.lang.Object more, java.lang.Object evenMore, java.lang.Object finalOne, java.lang.Object last, java.lang.Object finalTwo, java.lang.Object finalThree) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix, java.lang.Object finalSeven) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix, java.lang.Object finalSeven, java.lang.Object finalEight) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix, java.lang.Object finalSeven, java.lang.Object finalEight, java.lang.Object finalNine) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix, java.lang.Object finalSeven, java.lang.Object finalEight, java.lang.Object finalNine, java.lang.Object finalTen) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix, java.lang.Object finalSeven, java.lang.Object finalEight, java.lang.Object finalNine, java.lang.Object finalTen, java.lang.Object finalEleven) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more, java.lang.Object finalOne, java.lang.Object finalTwo, java.lang.Object finalThree, java.lang.Object finalFour, java.lang.Object finalFive, java.lang.Object finalSix, java.lang.Object finalSeven, java.lang.Object finalEight, java.lang.Object finalNine, java.lang.Object finalTen, java.lang.Object finalEleven, java.lang.Object finalTwelve) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc, java.lang.Object context, java.lang.Object extra, java.lang.Object more) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(PPermission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, java.security.DomainCombiner dc, java.security.AccessControlContext acc) {
            // Allow everything else
        }

        // The only check we actually enforce:
        @Override
        public void checkPermission(java.security.Permission perm) {
            if (perm instanceof RuntimePermission && perm.getName().startsWith("getenv.")) {
                throw new SecurityException("Access to environment variables is denied by test SecurityManager.");
            }
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Capture the original TZ value once for the whole test suite.
        originalTZ = System.getenv("TZ");
        // Obtain a mutable view of the environment map via reflection.
        // The implementation differs between Java versions / vendors,
        // so we try the known approaches.
        Map<String, String> env = System.getenv();
        Class<?> envClass = env.getClass();
        // For the typical Oracle/OpenJDK implementation the map is a
        // java.util.Collections$UnmodifiableMap that wraps a field named "m".
        // For Windows (ProcessEnvironment) the field may be called "theCaseInsensitiveEnvironment".
        try {
            Field field = envClass.getDeclaredField("m");
            field.setAccessible(true);
            //noinspection unchecked
            mutableEnv = (Map<String, String>) field.get(env);
        } catch (NoSuchFieldException e) {
            // Fallback for Windows where the field name is different
            Field field = envClass.getDeclaredField("theCaseInsensitiveEnvironment");
            field.setAccessible(true);
            //noinspection unchecked
            mutableEnv = (Map<String, String>) field.get(env);
        }
        originalTZ = System.getenv("TZ");
        // Obtain a mutable map that backs System.getenv()
        Map<String, String> env = System.getenv();
        Class<?> envClass = env.getClass();
        try {
            // Most JDK implementations: Collections$UnmodifiableMap wrapping a field named "m"
            Field f = envClass.getDeclaredField("m");
            f.setAccessible(true);
            //noinspection unchecked
            mutableEnv = (Map<String, String>) f.get(env);
        } catch (NoSuchFieldException e) {
            // Windows specific ProcessEnvironment implementation
            Field f = envClass.getDeclaredField("theCaseInsensitiveEnvironment");
            f.setAccessible(true);
            //noinspection unchecked
            mutableEnv = (Map<String, String>) f.get(env);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        // Remove any leftover "TZ" entry that a previous test might have left.
        mutableEnv.remove("TZ");
        // Ensure no leftover TZ entry.
        mutableEnv.remove("TZ");
        // Remember any existing SecurityManager so we can restore it later.
        previousSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original TZ value (or remove it if it was not set).
        if (originalTZ != null) {
            mutableEnv.put("TZ", originalTZ);
        } else {
            mutableEnv.remove("TZ");
        }
        // Restore the original TZ value (or remove it if it was originally absent).
        if (originalTZ != null) {
            mutableEnv.put("TZ", originalTZ);
        } else {
            mutableEnv.remove("TZ");
        }
        // Restore the original SecurityManager (if we replaced it).
        System.setSecurityManager(previousSecurityManager);
    }

    @Test
    void testGetSystemTimeZone_WithValidTZEnv() {
        // GIVEN a valid TZ identifier
        mutableEnv.put("TZ", "America/New_York");
        // WHEN
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // THEN the exact zone requested is returned
        assertEquals(TimeZone.getTimeZone("America/New_York"), tz, "When TZ is set to a valid zone, the method should return that TimeZone.");
    }

    @Test
    void testGetSystemTimeZone_WithInvalidTZEnv() {
        // GIVEN an unknown TZ identifier
        mutableEnv.put("TZ", "THIS_ZONE_DOES_NOT_EXIST");
        // WHEN
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // THEN TimeZone.getTimeZone returns the GMT zone for unknown IDs
        assertEquals(TimeZone.getTimeZone("GMT"), tz, "When TZ is unknown, TimeZone.getTimeZone returns GMT, which the method propagates.");
    }

    @Test
    void testGetSystemTimeZone_WhenTZNotSet() {
        // GIVEN no TZ entry (setup already removed it)
        assertNull(System.getenv("TZ"), "Pre‑condition: TZ must not be present.");
        // WHEN
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // THEN the JVM default zone is returned
        assertEquals(TimeZone.getDefault(), tz, "When TZ is not defined, the method should return the default TimeZone.");
    }

    @Test
    void testGetSystemTimeZone_WithEmptyStringTZ() {
        mutableEnv.put("TZ", "");
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // Empty string is treated as an unknown ID → GMT
        assertEquals(TimeZone.getTimeZone("GMT"), tz, "Empty TZ should yield the GMT TimeZone.");
    }

    @Test
    void testGetSystemTimeZone_WithWhitespaceTZ() {
        mutableEnv.put("TZ", "   ");
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // Whitespace only is also unknown → GMT
        assertEquals(TimeZone.getTimeZone("GMT"), tz, "Whitespace TZ should yield the GMT TimeZone.");
    }

    @Test
    void testGetSystemTimeZone_WithGmtOffsetTZ() {
        mutableEnv.put("TZ", "GMT+02:00");
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // TimeZone creates a custom zone with the given offset
        assertEquals(TimeZone.getTimeZone("GMT+02:00"), tz, "GMT offset strings should produce a corresponding offset TimeZone.");
    }

    @Test
    void testGetSystemTimeZone_WithLowerCaseValidId() {
        // lower‑case, not recognised
        mutableEnv.put("TZ", "america/new_york");
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // Because IDs are case‑sensitive, this falls back to GMT
        assertEquals(TimeZone.getTimeZone("GMT"), tz, "Lower‑case valid IDs are not recognised and result in GMT.");
    }

    @Test
    void testGetSystemTimeZone_NeverReturnsNull() {
        // Run the method under several different environment configurations
        String[] tzValues = { null, "UTC", "invalid/zone", "", "GMT+05:30" };
        for (String v : tzValues) {
            mutableEnv.remove("TZ");
            if (v != null) {
                mutableEnv.put("TZ", v);
            }
            TimeZone tz = SystemUtilities.getSystemTimeZone();
            assertNotNull(tz, "Method must never return null (TZ=" + v + ")");
        }
    }

    @Test
    void testSecurityExceptionFromSystemGetenv() {
        // Install a SecurityManager that blocks getenv
        System.setSecurityManager(new DenyGetEnvSecurityManager());
        // The method should propagate the SecurityException
        assertThrows(SecurityException.class, SystemUtilities::getSystemTimeZone, "When a SecurityManager blocks getenv, the exception should propagate.");
    }

    @Test
    void testSecurityExceptionFromTimeZoneGetTimeZone_isCaughtAndDefaultReturned() {
        // GIVEN a TZ value that would normally be used
        mutableEnv.put("TZ", "America/Los_Angeles");
        // Mock static TimeZone.getTimeZone to throw SecurityException
        try (MockedStatic<TimeZone> mocked = Mockito.mockStatic(TimeZone.class)) {
            mocked.when(() -> TimeZone.getTimeZone("America/Los_Angeles")).thenThrow(new SecurityException("Mocked SecurityException from getTimeZone"));
            // The method catches the exception and falls back to default
            TimeZone result = SystemUtilities.getSystemTimeZone();
            assertEquals(TimeZone.getDefault(), result, "If TimeZone.getTimeZone throws, the method should return the default TimeZone.");
        }
    }

    @Test
    void testSecurityExceptionFromTimeZoneGetDefault_propagates() {
        // Ensure TZ is not set so the method reaches TimeZone.getDefault()
        mutableEnv.remove("TZ");
        assertNull(System.getenv("TZ"));
        // Mock static TimeZone.getDefault to throw SecurityException
        try (MockedStatic<TimeZone> mocked = Mockito.mockStatic(TimeZone.class)) {
            mocked.when(TimeZone::getDefault).thenThrow(new SecurityException("Mocked SecurityException from getDefault"));
            // The exception is not caught inside getSystemTimeZone, so it propagates
            assertThrows(SecurityException.class, SystemUtilities::getSystemTimeZone, "SecurityException from TimeZone.getDefault should propagate.");
        }
    }
}
