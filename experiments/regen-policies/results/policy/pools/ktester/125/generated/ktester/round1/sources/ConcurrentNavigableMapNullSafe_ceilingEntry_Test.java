package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_ceilingEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class BrokenMap<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

        BrokenMap() {
            super();
        }
    }

    class BrokenMap<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

        BrokenMap() {
            super();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared after each test to avoid cross‑test contamination
        map.clear();
    }

    /**
     * Verify that ceilingEntry returns the exact entry whose key is equal to the supplied key.
     */
    @Test
    void ceilingEntryExactMatch() {
        map.put("a", 1);
        map.put("b", 2);
        // null is considered last
        map.put(null, 99);
        Entry<String, Integer> entry = map.ceilingEntry("a");
        assertNotNull(entry);
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
    }

    /**
     * Verify that ceilingEntry returns the next higher key when the supplied key is not present.
     */
    @Test
    void ceilingEntryNextHigherKey() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);
        // "aa" lies between "a" and "b"
        Entry<String, Integer> entry = map.ceilingEntry("aa");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
    }

    /**
     * Verify that when the supplied key is greater than any non‑null key,
     * the entry with the null key (the logical last key) is returned.
     */
    @Test
    void ceilingEntryReturnsNullKeyAsLast() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        // greater than "b"
        Entry<String, Integer> entry = map.ceilingEntry("z");
        assertNotNull(entry);
        // null key
        assertNull(entry.getKey());
        assertEquals(99, entry.getValue());
    }

    /**
     * Verify that ceilingEntry(null) returns the entry for the null key when it exists.
     */
    @Test
    void ceilingEntryWithNullKeyPresent() {
        map.put("a", 1);
        map.put(null, 42);
        Entry<String, Integer> entry = map.ceilingEntry(null);
        assertNotNull(entry);
        assertNull(entry.getKey());
        assertEquals(42, entry.getValue());
    }

    /**
     * Verify that ceilingEntry(null) returns {@code null} when the map does not contain a null key.
     * Since null is considered the greatest key, there is no entry greater than or equal to null.
     */
    @Test
    void ceilingEntryWithNullKeyAbsent() {
        map.put("a", 1);
        map.put("b", 2);
        Entry<String, Integer> entry = map.ceilingEntry(null);
        assertNull(entry);
    }

    /**
     * Verify that the returned {@link Entry} is a view of the internal entry:
     * modifying the value through the entry updates the map.
     */
    @Test
    void ceilingEntryReturnedEntryIsMutable() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        Entry<String, Integer> entry = map.ceilingEntry("a");
        assertNotNull(entry);
        assertEquals(1, entry.getValue());
        // mutate via the entry
        entry.setValue(10);
        assertEquals(10, map.get("a"));
    }

    /**
     * When the map is empty any key (including non‑null) should yield {@code null}.
     */
    @Test
    void ceilingEntryEmptyMap() {
        Entry<String, Integer> entry = map.ceilingEntry("A");
        assertNull(entry);
    }

    /**
     * Null key without a null entry: the method should return the smallest
     * non‑null key (i.e., behave like a normal ceiling search after masking).
     */
    @Test
    void ceilingEntryNullKeyWithoutNullEntry() {
        map.put("A", 10);
        map.put("B", 20);
        // No null key present
        Entry<String, Integer> entry = map.ceilingEntry(null);
        assertNotNull(entry);
        assertEquals("A", entry.getKey());
        assertEquals(10, entry.getValue());
    }

    /**
     * Empty‑string key handling – the sentinel used for null keys must not
     * interfere with an actual empty string key.
     */
    @Test
    void ceilingEntryEmptyStringKey() {
        map.put("", 0);
        map.put("a", 1);
        Entry<String, Integer> entry = map.ceilingEntry("");
        assertNotNull(entry);
        assertEquals("", entry.getKey());
        assertEquals(0, entry.getValue());
    }

    /**
     * Reverse‑order comparator: verify that ceilingEntry respects the custom order.
     */
    @Test
    void ceilingEntryCustomComparatorReverseOrder() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put("A", 1);
        revMap.put("C", 3);
        // In reverse order, "B" is considered greater than "C" and less than "A".
        // The ceilingEntry for "B" should therefore be "C".
        Entry<String, Integer> entry = revMap.ceilingEntry("B");
        assertNotNull(entry);
        assertEquals("C", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    /**
     * Integer keys: verify normal numeric ordering.
     */
    @Test
    void ceilingEntryIntegerKeys() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        Entry<Integer, String> entry = intMap.ceilingEntry(4);
        assertNotNull(entry);
        assertEquals(5, entry.getKey());
        assertEquals("five", entry.getValue());
    }

    /**
     * ClassCastException: map contains Integer keys, a String key is supplied.
     */
    @Test
    void ceilingEntryClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // The map expects Integer keys; passing a String triggers a ClassCastException
        assertThrows(ClassCastException.class, () -> intMap.ceilingEntry("stringKey"));
    }

    /**
     * Comparator that throws a RuntimeException for a specific value.
     */
    @Test
    void ceilingEntryComparatorThrowsException() {
        Comparator<String> badComparator = (o1, o2) -> {
            if ("bad".equals(o1) || "bad".equals(o2)) {
                throw new RuntimeException("Comparator deliberately fails on 'bad'");
            }
            return o1.compareTo(o2);
        };
        ConcurrentNavigableMapNullSafe<String, Integer> badMap = new ConcurrentNavigableMapNullSafe<>(badComparator);
        badMap.put("good", 1);
        badMap.put("better", 2);
        // Calling ceilingEntry with the problematic key should propagate the RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () -> badMap.ceilingEntry("bad"));
        assertEquals("Comparator deliberately fails on 'bad'", ex.getMessage());
    }

    /**
     * Simulate a subclass where the protected {@code internalMap} field is {@code null}.
     * Invoking {@code ceilingEntry} should result in a NullPointerException.
     */
    @Test
    void ceilingEntryNullInternalMap() throws Exception {
        // Subclass that does not initialise the internal map
        class BrokenMap<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

            BrokenMap() {
                super();
            }
        }
        BrokenMap<String, Integer> broken = new BrokenMap<>();
        // Use reflection to set the protected internalMap field to null
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(broken, null);
        // The method should now throw NPE when it attempts to cast/call on the null map
        assertThrows(NullPointerException.class, () -> broken.ceilingEntry("any"));
    }

    @ParameterizedTest(name = "ceilingEntry({0}) → expected key \"{1}\"")
    @CsvSource({ // key, expectedKey (null means the method should return null)
    // exact match
    "'A', A", // B absent, next higher is C
    "'B', C", // Z greater than any key → null
    "'Z', ", // empty string key (present)
    "'', ''" })
    void ceilingEntryParameterized(String key, String expectedKey) {
        // common map for the parameterized test
        map.put("A", 10);
        map.put("C", 30);
        // ensure empty string is present
        map.put("", 0);
        Entry<String, Integer> entry = map.ceilingEntry(key);
        if (expectedKey == null || expectedKey.isEmpty()) {
            // When expectedKey is empty string we differentiate between empty key and null result
            if (key.isEmpty()) {
                assertNotNull(entry);
                assertEquals("", entry.getKey());
                assertEquals(0, entry.getValue());
            } else {
                assertNull(entry);
            }
        } else {
            assertNotNull(entry);
            assertEquals(expectedKey, entry.getKey());
        }
    }
}
