package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_replace_Test {

    private TestMap map;

    // used by the pre‑existing tests
    private IntTestMap intMap;

    /**
     * concrete implementation used only for testing
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete implementation used for the original integer‑based tests
     */
    private static class IntTestMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        IntTestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete implementation used for the string‑based tests (covers null handling)
     */
    private static class StringTestMap extends AbstractConcurrentNullSafeMap<String, String> {

        StringTestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intMap = new IntTestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared between tests
        intMap.clear();
    }

    /**
     * Helper to create a fresh StringTestMap for each parameterized test
     */
    private StringTestMap newStringMap() {
        return new StringTestMap();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// key exists with matching oldValue
        Arguments.of("Key exists with matching oldValue, replace should succeed", "testKey", "old", "new", true, "old"), // key exists but oldValue does not match
        Arguments.of("Key exists but oldValue does not match, replace should fail", "testKey", "wrong", "new", false, "old"), // null key with matching oldValue
        Arguments.of("Null key is masked internally; replace returns true when old matches", null, "old", "new", true, "old"), // old value is null and stored value is null
        Arguments.of("Old value is null (masked). Stored value is null, replace succeeds", "testKey", null, "new", true, null), // missing key
        Arguments.of("Key does not exist, replace should return false", "missingKey", "any", "new", false, null), // replace existing value with null
        Arguments.of("Replace existing value with null, should succeed", "testKey", "old", null, true, "old"));
    }

    private static Stream<Arguments> nullEdgeCasesProvider() {
        return Stream.of(// map contains null key with null value, replace null with non‑null
        Arguments.of("Map contains a masked null key with a masked null value. Replacing null with a non‑null value should succeed", null, null, "newValue", true, null), // map contains null key with null value, replace null with null (no change)
        Arguments.of("Map contains a masked null key with a masked null value. Replacing null with null should still return true", null, null, null, true, null), // map contains null key with "oldValue", replace with "newValue"
        Arguments.of("Map contains a masked null key with value \"oldValue\". Matching oldValue, different newValue should replace", null, "oldValue", "newValue", true, "oldValue"), // map does NOT contain a null key, replace should fail
        Arguments.of("Map does NOT contain a null key. Attempting replace with any oldValue should fail", null, "oldValue", "newValue", false, // initialValue null => key absent
        null));
    }

    /**
     * replace succeeds when the current value matches the expected old value
     */
    @Test
    void replaceWhenOldValueMatches() {
        // given
        intMap.put("a", 1);
        // when
        boolean result = intMap.replace("a", 1, 2);
        // then
        assertTrue(result, "replace should return true when old value matches");
        assertEquals(2, intMap.get("a"));
    }

    /**
     * replace fails when the current value does not match the expected old value
     */
    @Test
    void replaceWhenOldValueDoesNotMatch() {
        // given
        intMap.put("b", 10);
        // when
        boolean result = intMap.replace("b", 5, 20);
        // then
        assertFalse(result, "replace should return false when old value does not match");
        assertEquals(10, intMap.get("b"), "value must remain unchanged");
    }

    /**
     * replace works with a null key and a null old value
     */
    @Test
    void replaceNullKeyWithNullOldValue() {
        // given
        intMap.put(null, 100);
        // when
        boolean result = intMap.replace(null, 100, 200);
        // then
        assertTrue(result, "replace should succeed for null key when old value matches");
        assertEquals(200, intMap.get(null));
    }

    /**
     * replace fails for a null key when the old value does not match
     */
    @Test
    void replaceNullKeyWithMismatchedOldValue() {
        // given
        intMap.put(null, 300);
        // when
        boolean result = intMap.replace(null, 999, 400);
        // then
        assertFalse(result, "replace should fail for null key when old value does not match");
        assertEquals(300, intMap.get(null), "value for null key must stay unchanged");
    }

    /**
     * replace returns false when the key is absent
     */
    @Test
    void replaceAbsentKey() {
        // given: map is empty
        // when
        boolean result = intMap.replace("missing", 1, 2);
        // then
        assertFalse(result, "replace should return false when key is not present");
        assertNull(intMap.get("missing"));
    }

    /**
     * Basic functionality group
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFunctionalityProvider")
    void replaceBasicFunctionality(String description, String key, String oldValue, String newValue, Boolean expectedResult, String initialValue) {
        StringTestMap map = newStringMap();
        // Populate the map according to the test case description
        if (initialValue != null) {
            map.put(key, initialValue);
        }
        boolean result = map.replace(key, oldValue, newValue);
        assertEquals(expectedResult, result, description);
        if (expectedResult) {
            // When replace succeeds, the new value must be stored (including null)
            assertEquals(newValue, map.get(key), "value after successful replace");
        } else {
            // When replace fails, the original value must stay unchanged
            assertEquals(initialValue, map.get(key), "value after failed replace");
        }
    }

    /**
     * Null handling edge cases group
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullEdgeCasesProvider")
    void replaceNullEdgeCases(String description, String key, String oldValue, String newValue, Boolean expectedResult, String initialValue) {
        StringTestMap map = newStringMap();
        // Populate map only when we want the key to be present
        if (initialValue != null) {
            map.put(key, initialValue);
        }
        boolean result = map.replace(key, oldValue, newValue);
        assertEquals(expectedResult, result, description);
        if (expectedResult) {
            assertEquals(newValue, map.get(key), "value after successful replace");
        } else {
            assertEquals(initialValue, map.get(key), "value after failed replace");
        }
    }

    /**
     * No‑op replacement (oldValue equals newValue)
     */
    @Test
    void replaceNoOpWhenOldEqualsNew() {
        StringTestMap map = newStringMap();
        map.put("testKey", "sameValue");
        boolean result = map.replace("testKey", "sameValue", "sameValue");
        assertTrue(result, "Replacing with the same value should succeed");
        assertEquals("sameValue", map.get("testKey"), "Value must remain unchanged");
    }

    /**
     * Empty map behavior
     */
    @Test
    void replaceOnEmptyMap() {
        // empty map
        StringTestMap map = newStringMap();
        boolean result = map.replace("anyKey", "anyOld", "anyNew");
        assertFalse(result, "Replace on an empty map must fail");
        assertNull(map.get("anyKey"));
    }

    /**
     * Mismatched old value when stored value is null
     */
    @Test
    void replaceMismatchedOldWhenStoredIsNull() {
        StringTestMap map = newStringMap();
        // stored null
        map.put("testKey", null);
        boolean result = map.replace("testKey", "nonNullOld", "newValue");
        assertFalse(result, "Non‑null oldValue must not match stored null value");
        assertNull(map.get("testKey"));
    }

    /**
     * Replace null with null on existing key
     */
    @Test
    void replaceNullWithNullOnExistingKey() {
        StringTestMap map = newStringMap();
        map.put("testKey", null);
        boolean result = map.replace("testKey", null, null);
        assertTrue(result, "Replacing null with null on an existing key should succeed");
        assertNull(map.get("testKey"));
    }

    /**
     * Internal map reference is null → NullPointerException
     */
    @Test
    void replaceThrowsWhenInternalMapIsNull() throws Exception {
        // Create a subclass that calls super with a valid map, then null‑out the field via reflection
        StringTestMap map = new StringTestMap();
        // Use reflection to set the final field `internalMap` to null
        Field field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, null);
        assertThrows(NullPointerException.class, () -> map.replace("anyKey", "old", "new"), "Calling replace when internalMap is null should throw NPE");
    }

    /**
     * Underlying map does not support replace → UnsupportedOperationException
     */
    @Test
    void replacePropagatesUnsupportedOperationException() {
        // Mock a ConcurrentMap that throws UnsupportedOperationException on replace
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockMap = mock(ConcurrentMap.class);
        when(mockMap.replace(any(), any(), any())).thenThrow(new UnsupportedOperationException("replace not supported"));
        // Subclass that injects the mocked map
        AbstractConcurrentNullSafeMap<String, String> map = new AbstractConcurrentNullSafeMap<String, String>(mockMap) {
        };
        assertThrows(UnsupportedOperationException.class, () -> map.replace("anyKey", "old", "new"), "UnsupportedOperationException should propagate from internalMap.replace");
    }
}
