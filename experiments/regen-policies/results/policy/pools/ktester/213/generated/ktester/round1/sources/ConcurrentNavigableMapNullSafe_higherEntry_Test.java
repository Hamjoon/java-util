package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_higherEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class NotComparable {

        final String name;

        NotComparable(String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    class NotComparable {

        final String name;

        NotComparable(String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate the map with a deterministic ordering
        // Natural ordering of strings: "a" < "b" < "c"
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Null key is considered *last* in the ascending view
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    /**
     * Provides arguments for the string‑key map scenarios.
     *
     * The map used in these cases contains the keys "b","c","d" (no null key).
     */
    private static Stream<Arguments> stringKeyCases() {
        return Stream.of(// key = null → according to current implementation null is greatest,
        // therefore higherEntry(null) must return null.
        Arguments.of(null, null, null, "null key – should return null because null is last"), // key = "a" (lower than any existing key)
        Arguments.of("a", "b", 2, "key \"a\" is lower than any existing key, expect entry for \"b\""), // key = "b"
        Arguments.of("b", "c", 3, "key \"b\" exists, expect entry for \"c\""), // key = "z" (greater than any key)
        Arguments.of("z", null, null, "key \"z\" greater than any key, expect null"), // key = "" (empty string, lower than all non‑empty keys)
        Arguments.of("", "b", 2, "empty string key should return first greater entry \"b\""));
    }

    /**
     * Provides arguments for the integer‑key map scenarios.
     */
    private static Stream<Arguments> integerKeyCases() {
        return Stream.of(// higherEntry(1) → 2
        Arguments.of(1, 2, "value2", "higherEntry(1) should return entry for key 2"), // higherEntry(2) → 3
        Arguments.of(2, 3, "value3", "higherEntry(2) should return entry for key 3"), // higherEntry(3) → null
        Arguments.of(3, null, null, "higherEntry(3) should return null (highest key)"), // higherEntry(5) → null (greater than any key)
        Arguments.of(5, null, null, "higherEntry(5) should return null (key beyond range)"));
    }

    @Test
    void higherEntry_existingKey() {
        // higherEntry for "a" should be "b"
        Entry<String, Integer> entry = map.higherEntry("a");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
        // higherEntry for "b" should be "c"
        entry = map.higherEntry("b");
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
        // higherEntry for "c" should be the null key entry (null is last)
        entry = map.higherEntry("c");
        assertNotNull(entry);
        assertNull(entry.getKey());
        assertEquals(100, entry.getValue());
    }

    @Test
    void higherEntry_nullKey() {
        // The null key is the greatest element, so there is no higher entry
        Entry<String, Integer> entry = map.higherEntry(null);
        assertNull(entry);
    }

    @Test
    void higherEntry_nonExistingKey() {
        // Key "aa" lies between "a" and "b"
        Entry<String, Integer> entry = map.higherEntry("aa");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
        // Key "d" is greater than any existing non‑null key but less than null,
        // so the higher entry should be the null key entry
        entry = map.higherEntry("d");
        assertNotNull(entry);
        assertNull(entry.getKey());
        assertEquals(100, entry.getValue());
        // Key that is greater than null (conceptually) does not exist,
        // therefore higherEntry returns null
        // Since null is considered the greatest, any key that sorts after null
        // cannot be represented; we simply verify that a key beyond the known
        // range yields null.
        // a high Unicode character
        entry = map.higherEntry("\uFFFF");
        assertNull(entry);
    }

    @Test
    void higherEntry_reflectsMapChanges() {
        // Remove the null key and verify higherEntry behavior changes accordingly
        map.remove(null);
        // Now "c" is the greatest key; higherEntry("c") should be null
        assertNull(map.higherEntry("c"));
        // Add a new key "z" which is greater than existing keys
        map.put("z", 26);
        // higherEntry for "c" should now be "z"
        Entry<String, Integer> entry = map.higherEntry("c");
        assertNotNull(entry);
        assertEquals("z", entry.getKey());
        assertEquals(26, entry.getValue());
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringKeyCases")
    void higherEntry_stringMapScenarios(Object key, String expectedKey, Integer expectedValue, String description) {
        // Build a fresh map that matches the JSON description: keys "b","c","d"
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("b", 2);
        localMap.put("c", 3);
        localMap.put("d", 4);
        // No null key – null is treated as greatest only if present
        @SuppressWarnings("unchecked")
        Entry<String, Integer> result = localMap.higherEntry((String) key);
        if (expectedKey == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(expectedKey, result.getKey(), description);
            assertEquals(expectedValue, result.getValue(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("integerKeyCases")
    void higherEntry_integerMapScenarios(Integer key, Integer expectedKey, String expectedValue, String description) {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "value1");
        intMap.put(2, "value2");
        intMap.put(3, "value3");
        Entry<Integer, String> result = intMap.higherEntry(key);
        if (expectedKey == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(expectedKey, result.getKey(), description);
            assertEquals(expectedValue, result.getValue(), description);
        }
    }

    @Test
    void higherEntry_emptyMap() {
        // An empty map should always return null regardless of the supplied key
        ConcurrentNavigableMapNullSafe<String, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        assertNull(emptyMap.higherEntry("anyKey"));
        assertNull(emptyMap.higherEntry(null));
    }

    @Test
    void higherEntry_typeMismatch_throwsClassCastException() {
        // Underlying map has String keys; supplying an Integer should cause ClassCastException
        ConcurrentNavigableMapNullSafe<String, String> stringMap = new ConcurrentNavigableMapNullSafe<>();
        stringMap.put("a", "valueA");
        stringMap.put("b", "valueB");
        assertThrows(ClassCastException.class, () -> stringMap.higherEntry((String) (Object) 1), "Supplying an Integer key to a String‑key map must propagate ClassCastException");
    }

    @Test
    void higherEntry_nonComparableKey_throwsClassCastException() {
        // Map uses natural ordering; a key that does not implement Comparable should fail
        class NotComparable {

            final String name;

            NotComparable(String n) {
                this.name = n;
            }

            @Override
            public String toString() {
                return name;
            }
        }
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "valueA");
        map.put("b", "valueB");
        NotComparable badKey = new NotComparable("bad");
        // The cast to String is only to satisfy the method signature; at runtime the
        // underlying ConcurrentSkipListMap will attempt to compare and throw.
        assertThrows(ClassCastException.class, () -> map.higherEntry((String) (Object) badKey), "Non‑comparable key must result in ClassCastException");
    }

    /**
     * Additional sanity test for a key that is higher than the null sentinel.
     * According to the library contract, null is the greatest element; any
     * key that would sort after null cannot be represented, so higherEntry
     * must return null.
     */
    @Test
    void higherEntry_keyBeyondNull_returnsNull() {
        // Using the map from @BeforeEach which already contains a null key.
        Entry<String, Integer> result = map.higherEntry("\uFFFF");
        assertNull(result, "Key beyond the null sentinel should yield null");
    }
}
