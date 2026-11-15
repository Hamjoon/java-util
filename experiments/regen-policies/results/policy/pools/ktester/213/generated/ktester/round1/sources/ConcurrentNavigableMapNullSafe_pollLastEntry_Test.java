package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_pollLastEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /* -------------------------------------------------------------
     *  Helper holder classes for parameterized test data
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String[] keys;

        final ExpectedEntry<String, Integer> expected;

        // not used here but kept for extensibility
        final String expectedException;

        final String description;

        ArgumentsHolder(String[] keys, ExpectedEntry<String, Integer> expected, String expectedException, String description) {
            this.keys = keys;
            this.expected = expected;
            this.expectedException = expectedException;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static class ExpectedEntry<K, V> {

        final K key;

        final V value;

        ExpectedEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test case
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the template tidy
        map = null;
    }

    private static Stream<ArgumentsHolder> pollLastEntryScenarios() {
        return Stream.of(// empty map -> null
        new ArgumentsHolder(new String[] {}, null, "null", "When the underlying ConcurrentSkipListMap is empty, pollLastEntry() should return null because there is no last entry to retrieve."), // normal entries -> highest key "c"
        new ArgumentsHolder(new String[] { "a", "b", "c" }, new ExpectedEntry<>("c", 3), null, "When the map contains several entries, pollLastEntry() must return the entry with the highest key according to the map's natural ordering."), // null value case -> key "z" with null value
        new ArgumentsHolder(new String[] { "x", "z" }, new ExpectedEntry<>("z", null), null, "When the map contains an entry whose value is null, pollLastEntry() should still return the correct entry (the one with the highest key) while preserving the null value."));
    }

    @Test
    void pollLastEntryReturnsAndRemovesHighestKeyWhenNoNullKey() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        // highest key in natural order
        map.put("c", 3);
        // when
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        // then
        assertNotNull(entry, "pollLastEntry should return the last entry");
        assertEquals("c", entry.getKey(), "The key of the returned entry should be the highest key");
        assertEquals(3, entry.getValue());
        // The entry must have been removed from the map
        assertNull(map.get("c"));
        assertEquals(2, map.size());
    }

    @Test
    void pollLastEntryReturnsAndRemovesNullKeyWhenPresent() {
        // given
        map.put("x", 10);
        // null key is considered *last* according to the class contract
        map.put(null, 99);
        map.put("y", 20);
        // when
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        // then
        assertNotNull(entry, "pollLastEntry should return an entry when map contains a null key");
        assertNull(entry.getKey(), "The returned entry should have a null key");
        assertEquals(99, entry.getValue());
        // The null‑key entry must have been removed
        assertNull(map.get(null));
        assertEquals(2, map.size());
    }

    @Test
    void pollLastEntryReturnsNullWhenMapIsEmpty() {
        // given: an empty map (setupBeforeEach already created an empty instance)
        // when
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        // then
        assertNull(entry, "pollLastEntry should return null for an empty map");
    }

    @Test
    void pollLastEntryHandlesNullValueCorrectly() {
        // given a map where the highest key maps to a null value
        map.put("x", 1);
        // highest key, null value
        map.put("z", null);
        // when
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        // then
        assertNotNull(entry, "Entry should be returned even if its value is null");
        assertEquals("z", entry.getKey(), "Key should be the highest one");
        assertNull(entry.getValue(), "Value should be null as stored");
        // verify removal
        assertNull(map.get("z"));
        assertEquals(1, map.size());
    }

    @Test
    void pollLastEntryThrowsNPEWhenInternalMapIsNull() throws Exception {
        // Force the private internalMap field to null
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // when / then
        assertThrows(NullPointerException.class, () -> map.pollLastEntry(), "Calling pollLastEntry with a null internalMap should throw NPE");
    }

    @Test
    void pollLastEntryThrowsClassCastExceptionWhenInternalMapNotSkipList() throws Exception {
        // Replace internalMap with a plain ConcurrentHashMap (not a ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new java.util.concurrent.ConcurrentHashMap<>());
        // when / then
        assertThrows(ClassCastException.class, () -> map.pollLastEntry(), "Calling pollLastEntry with an internalMap of wrong type should throw CCE");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("pollLastEntryScenarios")
    void pollLastEntryParameterized(ArgumentsHolder args) {
        // populate map according to the scenario
        for (String key : args.keys) {
            if ("x".equals(key)) {
                map.put(key, 1);
            } else if ("z".equals(key)) {
                map.put(key, null);
            } else if ("a".equals(key)) {
                map.put(key, 1);
            } else if ("b".equals(key)) {
                map.put(key, 2);
            } else if ("c".equals(key)) {
                map.put(key, 3);
            } else {
                map.put(key, 0);
            }
        }
        Map.Entry<String, Integer> result = map.pollLastEntry();
        if (args.expected == null) {
            assertNull(result, "Expected null result");
        } else {
            assertNotNull(result, "Result should not be null");
            assertEquals(args.expected.key, result.getKey(), "Key mismatch");
            assertEquals(args.expected.value, result.getValue(), "Value mismatch");
            // ensure the entry was removed
            assertNull(map.get(args.expected.key), "Entry should have been removed from the map");
        }
    }
}
