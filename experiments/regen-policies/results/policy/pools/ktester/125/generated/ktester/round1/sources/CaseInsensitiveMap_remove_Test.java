package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_remove_Test {

    private CaseInsensitiveMap<Object, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor (LinkedHashMap backing) for a clean map.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the CaseInsensitiveString cache length to the documented default.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    static Stream<Arguments> stringKeyRemovalProvider() {
        return Stream.of(// exact case match
        Arguments.of("Exact‑case removal – should return value and delete entry", "TestKey", "valueExactCase", "TestKey", "valueExactCase"), // different case match
        Arguments.of("Different‑case removal – case‑insensitive match", "TestKey", "valueDifferentCase", "testkey", "valueDifferentCase"), // missing key
        Arguments.of("Missing key – should return null and leave map unchanged", "ExistingKey", "someValue", "MissingKey", null));
    }

    static Stream<Arguments> nonStringKeyRemovalProvider() {
        return Stream.of(Arguments.of("Existing Integer key – should be removed and value returned", 42, "answer", 42, "answer"), Arguments.of("Missing Integer key – should return null", 1, "one", 99, null));
    }

    static Stream<Arguments> nullKeyRemovalProvider() {
        return Stream.of(Arguments.of("Null key present – should return associated value", null, "nullValue", null, "nullValue"), Arguments.of("Null key absent – should return null", "someKey", "someValue", null, null));
    }

    @Test
    void removeStringKeyCaseInsensitive() {
        map.put("Key", "value");
        // Remove using a different case
        Object removed = map.remove("kEy");
        assertEquals("value", removed, "remove should return the associated value");
        assertNull(map.get("Key"), "Key should no longer be present");
        assertTrue(map.isEmpty(), "Map should be empty after removal");
    }

    @Test
    void removeStringKeyNotPresentReturnsNull() {
        map.put("Alpha", "A");
        // not present
        Object removed = map.remove("beta");
        assertNull(removed, "remove should return null when key is absent");
        assertEquals(1, map.size(), "Map size must remain unchanged");
        assertEquals("A", map.get("Alpha"));
    }

    @Test
    void removeNonStringKey() {
        map.put(42, "answer");
        map.put("Pi", "3.14");
        Object removed = map.remove(42);
        assertEquals("answer", removed, "remove should return the value for the non‑String key");
        assertFalse(map.containsKey(42), "The integer key should be removed");
        assertTrue(map.containsKey("Pi"), "Other entries must stay untouched");
        assertEquals(1, map.size(), "Only one entry should remain");
    }

    @Test
    void removeNullKey() {
        map.put(null, "nullValue");
        map.put("NotNull", "value");
        Object removed = map.remove(null);
        assertEquals("nullValue", removed, "remove should handle null keys");
        assertFalse(map.containsKey(null), "Null key must be removed");
        assertTrue(map.containsKey("NotNull"));
        assertEquals(1, map.size());
    }

    @Test
    void removeOnlyTargetedEntryWhenMultiplePresent() {
        map.put("One", "1");
        map.put("Two", "2");
        map.put("Three", "3");
        map.put(99, "ninety‑nine");
        // case‑insensitive match
        Object removed = map.remove("tWo");
        assertEquals("2", removed);
        assertFalse(map.containsKey("Two"));
        // Verify the rest of the map is intact
        assertEquals("1", map.get("ONE"));
        assertEquals("3", map.get("three"));
        assertEquals("ninety‑nine", map.get(99));
        assertEquals(3, map.size());
    }

    /**
     * Tests removal of {@link String} keys with various case combinations
     * and the case where the key is missing.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringKeyRemovalProvider")
    void removeStringKey(String description, String storedKey, Object storedValue, Object keyToRemove, Object expectedResult) {
        map.put(storedKey, storedValue);
        Object removed = map.remove(keyToRemove);
        assertEquals(expectedResult, removed, description);
        assertNull(map.get(storedKey), "The entry should have been removed");
    }

    @Test
    void removeDuplicateStringKeys() {
        // Insert two case‑variant keys. The map keeps insertion order,
        // and the first inserted key is expected to be removed first.
        map.put("DuplicateKey", "firstValue");
        map.put("duplicatekey", "secondValue");
        // Remove using any case – should delete the first inserted entry.
        Object firstRemoved = map.remove("DuplicateKey");
        assertEquals("firstValue", firstRemoved, "First removal should return the first value");
        assertFalse(map.containsKey("DuplicateKey"));
        assertTrue(map.containsKey("duplicatekey"));
        assertEquals(1, map.size());
        // Remove the remaining entry with a different case.
        Object secondRemoved = map.remove("duplicatekey");
        assertEquals("secondValue", secondRemoved, "Second removal should return the second value");
        assertTrue(map.isEmpty(), "Map should be empty after both removals");
    }

    @Test
    void removeNullKeyOnConcurrentHashMap_throwsNPE() {
        // Backing map that disallows null keys.
        CaseInsensitiveMap<Object, Object> concurrentMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        concurrentMap.put("valid", "value");
        // Expect NPE because ConcurrentHashMap.remove(null) throws it.
        assertThrows(NullPointerException.class, () -> concurrentMap.remove(null), "Removing null from a ConcurrentHashMap should throw NullPointerException");
    }

    @Test
    void removeNonStringKeyOnTreeMap_throwsClassCastException() {
        // TreeMap with natural ordering (expects comparable keys, i.e., Strings).
        CaseInsensitiveMap<Object, Object> treeMap = new CaseInsensitiveMap<>(new TreeMap<>());
        treeMap.put("alpha", "A");
        // Removing an Integer forces TreeMap to compare Integer with String -> CCE.
        assertThrows(ClassCastException.class, () -> treeMap.remove(123), "Removing a non‑String key from a TreeMap with String ordering should throw ClassCastException");
    }

    @Test
    void removeOnUnmodifiableMap_throwsUnsupportedOperationException() {
        Map<Object, Object> base = new HashMap<>();
        base.put("anyKey", "anyValue");
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(base);
        CaseInsensitiveMap<Object, Object> unmodifiableMap = new CaseInsensitiveMap<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.remove("anyKey"), "Removing from an unmodifiable backing map must throw UnsupportedOperationException");
    }
}
