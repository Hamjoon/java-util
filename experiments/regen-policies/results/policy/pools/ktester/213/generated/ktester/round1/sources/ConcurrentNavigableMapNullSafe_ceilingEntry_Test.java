package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_ceilingEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private ConcurrentNavigableMapNullSafe<String, Integer> stringMap;

    @BeforeEach
    void setupBeforeEach() {
        stringMap = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a predictable ordering for the String‑based tests
        stringMap.put("a", 1);
        stringMap.put("b", 2);
        stringMap.put("c", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        stringMap.clear();
    }

    /* --------------------------------------------------------------
     *  Integer‑based map – richer set of scenarios from the JSON spec
     * -------------------------------------------------------------- */
    private static Stream<Arguments> basicIntegerCases() {
        return Stream.of(// key = null → first entry (sentinel is considered less than any real key)
        Arguments.of(null, "one", 1), // exact match
        Arguments.of(2, "two", 2), // non‑existent key 3 → higher key 4
        Arguments.of(3, "four", 4), // key beyond highest → null
        Arguments.of(10, null, null));
    }

    /* --------------------------------------------------------------
     *  Helper – reflection based injection of internalMap
     * -------------------------------------------------------------- */
    private static void setInternalMap(ConcurrentNavigableMapNullSafe<?, ?> target, Object map) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, map);
    }

    @Test
    void ceilingEntry_exactMatch() {
        Entry<String, Integer> entry = map.ceilingEntry("b");
        assertNotNull(entry, "Ceiling entry for an existing key should not be null");
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
    }

    @Test
    void ceilingEntry_higherKey() {
        // "bb" is not present; the next higher key in natural order is "c"
        Entry<String, Integer> entry = map.ceilingEntry("bb");
        assertNotNull(entry, "Ceiling entry for a non‑existent key should return the next higher entry");
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void ceilingEntry_noHigherKeyWithoutNull() {
        // "z" is greater than any existing non‑null key and there is no null key in the map
        Entry<String, Integer> entry = map.ceilingEntry("z");
        assertNull(entry, "Ceiling entry should be null when no key is greater or equal (and no null key present)");
    }

    @Test
    void ceilingEntry_nullKeyPresent() {
        map.put(null, 100);
        // Null is considered the greatest key; ceilingEntry(null) should return the null entry itself
        Entry<String, Integer> entry = map.ceilingEntry(null);
        assertNotNull(entry, "Ceiling entry for null key when null key exists should not be null");
        assertNull(entry.getKey(), "Key of the returned entry should be null");
        assertEquals(100, entry.getValue());
    }

    @Test
    void ceilingEntry_nullKeyAbsent() {
        // Ensure null key is not present
        map.remove(null);
        Entry<String, Integer> entry = map.ceilingEntry(null);
        assertNull(entry, "Ceiling entry for null key when null key does not exist should be null");
    }

    @Test
    void ceilingEntry_nullKeyWithHigherNonNullKeys() {
        // Add a null key and verify that ceilingEntry for a key greater than all non‑null keys returns the null entry
        map.put(null, 999);
        Entry<String, Integer> entry = map.ceilingEntry("z");
        assertNotNull(entry, "Ceiling entry for a key beyond the highest non‑null key should return the null entry if present");
        assertNull(entry.getKey(), "Returned entry should have a null key");
        assertEquals(999, entry.getValue());
    }

    @ParameterizedTest(name = "ceilingEntry({0}) → {1}")
    @MethodSource("basicIntegerCases")
    void ceilingEntry_integerBasic(Object key, String expectedValue, Integer expectedKey) throws Exception {
        // Build a map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(4, "four");
        intMap.put(5, "five");
        // Invoke the method under test
        Entry<Integer, String> result = intMap.ceilingEntry((Integer) key);
        if (expectedKey == null) {
            assertNull(result, "Expected null result for key " + key);
        } else {
            assertNotNull(result);
            assertEquals(expectedKey, result.getKey());
            assertEquals(expectedValue, result.getValue());
        }
    }

    @Test
    void ceilingEntry_keyLessThanSmallest() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(4, "four");
        intMap.put(5, "five");
        Entry<Integer, String> entry = intMap.ceilingEntry(-5);
        assertNotNull(entry);
        assertEquals(1, entry.getKey());
        assertEquals("one", entry.getValue());
    }

    @Test
    void ceilingEntry_exactSmallestKey() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(4, "four");
        intMap.put(5, "five");
        Entry<Integer, String> entry = intMap.ceilingEntry(1);
        assertNotNull(entry);
        assertEquals(1, entry.getKey());
        assertEquals("one", entry.getValue());
    }

    @Test
    void ceilingEntry_mapContainsNullKeyEntry() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, "nullVal");
        intMap.put(1, "one");
        intMap.put(2, "two");
        Entry<Integer, String> entry = intMap.ceilingEntry(null);
        assertNotNull(entry);
        assertNull(entry.getKey());
        assertEquals("nullVal", entry.getValue());
    }

    /* --------------------------------------------------------------
     *  Reverse order comparator
     * -------------------------------------------------------------- */
    @Test
    void ceilingEntry_reverseComparator() throws Exception {
        // Create a ConcurrentSkipListMap with reverse order and inject it
        ConcurrentSkipListMap<Integer, String> reverseMap = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        reverseMap.put(5, "five");
        reverseMap.put(3, "three");
        reverseMap.put(1, "one");
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(intMap, reverseMap);
        // In reverse order, ceiling(2) = 3
        Entry<Integer, String> e1 = intMap.ceilingEntry(2);
        assertNotNull(e1);
        assertEquals(3, e1.getKey());
        assertEquals("three", e1.getValue());
        // Exact greatest element (5) should be returned
        Entry<Integer, String> e2 = intMap.ceilingEntry(5);
        assertNotNull(e2);
        assertEquals(5, e2.getKey());
        assertEquals("five", e2.getValue());
    }

    /* --------------------------------------------------------------
     *  ClassCastException cases
     * -------------------------------------------------------------- */
    @Test
    void ceilingEntry_keyOfDifferentComparableType() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        // Supplying a Long should trigger ClassCastException from the underlying map
        assertThrows(ClassCastException.class, () -> intMap.ceilingEntry(2L));
    }

    @Test
    void ceilingEntry_nonComparableKey() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        // new Object() does not implement Comparable
        assertThrows(ClassCastException.class, () -> intMap.ceilingEntry(new Object()));
    }

    /* --------------------------------------------------------------
     *  Single‑element map edge cases
     * -------------------------------------------------------------- */
    @Test
    void ceilingEntry_singleElement_lowerKey() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        Entry<Integer, String> entry = intMap.ceilingEntry(4);
        assertNotNull(entry);
        assertEquals(5, entry.getKey());
        assertEquals("five", entry.getValue());
    }

    @Test
    void ceilingEntry_singleElement_higherKey() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        Entry<Integer, String> entry = intMap.ceilingEntry(6);
        assertNull(entry);
    }

    /* --------------------------------------------------------------
     *  Internal map misuse – type mismatch & null internalMap
     * -------------------------------------------------------------- */
    @Test
    void ceilingEntry_internalMapTypeMismatch() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        // Inject a HashMap (not a ConcurrentSkipListMap)
        setInternalMap(intMap, new HashMap<>());
        assertThrows(ClassCastException.class, () -> intMap.ceilingEntry(1));
    }

    @Test
    void ceilingEntry_internalMapNull() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        // Force internalMap to null
        setInternalMap(intMap, null);
        assertThrows(NullPointerException.class, () -> intMap.ceilingEntry(1));
    }
}
