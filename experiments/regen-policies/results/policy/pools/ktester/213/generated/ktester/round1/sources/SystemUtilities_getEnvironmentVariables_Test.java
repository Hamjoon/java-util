package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.security.Permission;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

// kept for consistency with the template; not used here
@ExtendWith(MockitoExtension.class)
class SystemUtilities_getEnvironmentVariables_Test {

    /**
     * A key that is guaranteed to exist in the current process environment.
     */
    private static String existingKey;

    /**
     * The value associated with {@link #existingKey}.
     */
    private static String existingValue;

    @BeforeAll
    static void setupBeforeAll() {
        // Grab any entry from the real environment map – the map is never empty on a normal JVM.
        Map<String, String> env = System.getenv();
        assertFalse(env.isEmpty(), "The test environment must contain at least one variable");
        existingKey = env.keySet().iterator().next();
        existingValue = env.get(existingKey);
    }

    // Helper method used as a method reference in the next test
    private static boolean isOsNameKey(String key) {
        return SystemUtilities.OS_NAME.equals(key);
    }

    @Test
    void getAllWhenFilterIsNull_returnsCompleteEnvironment() {
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(null);
        // The result should contain exactly the same entries as the real environment.
        Map<String, String> real = System.getenv();
        assertEquals(real.size(), result.size(), "Result size must match real environment size");
        assertTrue(result.entrySet().containsAll(real.entrySet()), "Result must contain every entry from the real environment");
    }

    @Test
    void getOnlyMatchingKey_returnsSingleEntry() {
        // Predicate that matches only the known existing key.
        Predicate<String> filter = key -> existingKey.equals(key);
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(filter);
        assertEquals(1, result.size(), "Only one entry should be returned");
        assertTrue(result.containsKey(existingKey), "Result must contain the expected key");
        assertEquals(existingValue, result.get(existingKey), "Value must match the real environment");
    }

    @Test
    void getNoneWhenPredicateAlwaysFalse_returnsEmptyMap() {
        Predicate<String> neverTrue = key -> false;
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(neverTrue);
        assertNotNull(result, "Result map must never be null");
        assertTrue(result.isEmpty(), "Result should be empty when predicate never matches");
    }

    @Test
    void getMultipleWhenPredicateMatchesSubset_returnsCorrectSubset() {
        // Build a predicate that matches the first two keys (if at least two exist)
        String[] keys = System.getenv().keySet().toArray(new String[0]);
        if (keys.length < 2) {
            // Fallback – if the environment has only one key, test the single‑key case above.
            return;
        }
        String key1 = keys[0];
        String key2 = keys[1];
        Predicate<String> filter = key -> key.equals(key1) || key.equals(key2);
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(filter);
        assertEquals(2, result.size(), "Exactly two entries should be returned");
        assertEquals(System.getenv(key1), result.get(key1));
        assertEquals(System.getenv(key2), result.get(key2));
    }

    @Test
    void filterAlwaysTrue_returnsCompleteEnvironment() {
        Predicate<String> alwaysTrue = key -> true;
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(alwaysTrue);
        Map<String, String> real = System.getenv();
        assertEquals(real.size(), result.size(), "All entries must be present when predicate is always true");
        assertTrue(result.entrySet().containsAll(real.entrySet()));
    }

    @Test
    void selectiveFilter_homeCaseInsensitive_returnsOnlyHome() {
        // Find a key that equals "HOME" ignoring case
        Optional<String> homeKeyOpt = System.getenv().keySet().stream().filter(k -> k.equalsIgnoreCase("HOME")).findFirst();
        if (homeKeyOpt.isEmpty()) {
            // If the environment does not contain a HOME variable, the test is not applicable.
            // We simply assert that the result is empty.
            Map<String, String> result = SystemUtilities.getEnvironmentVariables(key -> key.equalsIgnoreCase("HOME"));
            assertTrue(result.isEmpty(), "No HOME variable present – result should be empty");
            return;
        }
        String homeKey = homeKeyOpt.get();
        Predicate<String> filter = key -> key.equalsIgnoreCase("HOME");
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(filter);
        assertEquals(1, result.size(), "Only the HOME entry should be returned");
        assertTrue(result.containsKey(homeKey));
        assertEquals(System.getenv(homeKey), result.get(homeKey));
    }

    @Test
    void selectiveFilter_startsWithJava_returnsJavaVariables() {
        Predicate<String> filter = key -> key.startsWith("JAVA");
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(filter);
        Map<String, String> real = System.getenv();
        // Verify every returned key starts with "JAVA"
        assertTrue(result.keySet().stream().allMatch(k -> k.startsWith("JAVA")));
        // Verify that the map contains exactly the same entries as the real env for those keys
        Map<String, String> expected = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : real.entrySet()) {
            if (e.getKey().startsWith("JAVA")) {
                expected.put(e.getKey(), e.getValue());
            }
        }
        assertEquals(expected, result);
    }

    @Test
    void predicateThrowsRuntimeException_exceptionPropagates() {
        Predicate<String> badPredicate = key -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> SystemUtilities.getEnvironmentVariables(badPredicate));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void resultMapIsLinkedHashMap_preservesInsertionOrder() {
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(null);
        assertTrue(result instanceof LinkedHashMap, "Result map must be a LinkedHashMap");
        // Verify order preservation by comparing the key iteration order with the source order
        List<String> sourceOrder = new ArrayList<>(System.getenv().keySet());
        List<String> resultOrder = new ArrayList<>(result.keySet());
        assertEquals(sourceOrder, resultOrder, "Iteration order must match the order of System.getenv()");
    }

    @Test
    void predicateSideEffectCounting_predicateInvokedForEveryEntry() {
        AtomicInteger callCount = new AtomicInteger(0);
        Predicate<String> countingPredicate = key -> {
            callCount.incrementAndGet();
            // always keep the entry
            return true;
        };
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(countingPredicate);
        Map<String, String> real = System.getenv();
        assertEquals(real.size(), result.size(), "Result should contain all entries");
        assertEquals(real.size(), callCount.get(), "Predicate must be invoked exactly once for each environment entry");
    }

    @Test
    void mutableResult_modificationsDoNotAffectSystemEnv() {
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(null);
        assertFalse(result.containsKey("NEW_VAR"), "Result should not already contain NEW_VAR");
        result.put("NEW_VAR", "value");
        assertEquals("value", result.get("NEW_VAR"), "Result map must reflect the new entry");
        assertNull(System.getenv("NEW_VAR"), "System.getenv() must not be affected by modifications");
    }

    @Test
    void filterByKeyLength_fourCharactersOnly() {
        Predicate<String> lengthFour = key -> key.length() == 4;
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(lengthFour);
        // Verify each key length
        assertTrue(result.keySet().stream().allMatch(k -> k.length() == 4), "All keys in the result must have length 4");
        // Verify that no other keys of length 4 are missing
        Set<String> expected = new LinkedHashSet<>();
        for (String k : System.getenv().keySet()) {
            if (k.length() == 4)
                expected.add(k);
        }
        assertEquals(expected, result.keySet(), "Result must contain exactly the keys of length 4, preserving order");
    }

    @Test
    void filterUsingRegex_patternMatchesPathOrHome() {
        Pattern pattern = Pattern.compile("^(PATH|HOME)$");
        Predicate<String> regexPredicate = key -> pattern.matcher(key).matches();
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(regexPredicate);
        // Build expected set based on real environment
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            if (pattern.matcher(e.getKey()).matches()) {
                expected.put(e.getKey(), e.getValue());
            }
        }
        assertEquals(expected, result);
    }

    @Test
    void filterAsMethodReference_selectsOsNameKey() {
        Predicate<String> methodRef = SystemUtilities_getEnvironmentVariables_Test::isOsNameKey;
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(methodRef);
        // The map may be empty if the OS name is not present as an environment variable
        if (System.getenv().containsKey(SystemUtilities.OS_NAME)) {
            assertEquals(1, result.size(), "Exactly one entry should be present");
            assertEquals(System.getenv(SystemUtilities.OS_NAME), result.get(SystemUtilities.OS_NAME));
        } else {
            assertTrue(result.isEmpty(), "Result should be empty when OS name variable is absent");
        }
    }

    @Test
    void securityManagerDeniesAccess_throwsSecurityException() {
        // Preserve the original security manager so we can restore it later
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive security manager that denies getenv()
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except getenv
                if (perm instanceof RuntimePermission && "getenv".equals(perm.getName())) {
                    throw new SecurityException("Access to environment variables is prohibited");
                }
                // otherwise allow
            }
        });
        try {
            // The call to System.getenv() inside the method should now throw SecurityException
            assertThrows(SecurityException.class, () -> SystemUtilities.getEnvironmentVariables(null));
        } finally {
            // Restore original security manager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
