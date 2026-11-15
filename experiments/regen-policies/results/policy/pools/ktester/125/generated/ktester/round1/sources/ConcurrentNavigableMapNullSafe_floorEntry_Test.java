package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_floorEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to guarantee isolation.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray references remain.
        map = null;
    }

    @Test
    void floorEntry_normalKey_returnsGreatestKeyLessThanOrEqual() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // when
        // between "c" and "e"
        Entry<String, Integer> entry = map.floorEntry("d");
        // then
        assertNotNull(entry, "floorEntry should return a non‑null entry for existing range");
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void floorEntry_exactMatch_returnsSameEntry() {
        // given
        map.put("alpha", 10);
        map.put("beta", 20);
        map.put("gamma", 30);
        // when
        Entry<String, Integer> entry = map.floorEntry("beta");
        // then
        assertNotNull(entry);
        assertEquals("beta", entry.getKey());
        assertEquals(20, entry.getValue());
    }

    @Test
    void floorEntry_nullKeyPresent_returnsNullKeyEntry() {
        // given
        map.put("x", 100);
        map.put("y", 200);
        // null key is considered the greatest key
        map.put(null, 999);
        // when
        Entry<String, Integer> entry = map.floorEntry(null);
        // then
        assertNotNull(entry);
        assertNull(entry.getKey(), "The key of the returned entry should be null");
        assertEquals(999, entry.getValue());
    }

    @Test
    void floorEntry_nullKeyAbsent_returnsGreatestNonNullKey() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // No null key inserted
        // when
        // null is considered greater than any non‑null key
        Entry<String, Integer> entry = map.floorEntry(null);
        // then
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void floorEntry_keySmallerThanAll_returnsNull() {
        // given
        map.put("m", 100);
        map.put("n", 200);
        map.put(null, 999);
        // when
        // smaller than the smallest key "m"
        Entry<String, Integer> entry = map.floorEntry("a");
        // then
        assertNull(entry, "floorEntry should return null when no key is less than or equal to the supplied key");
    }

    /**
     * 1. floorEntry with null key (null key present) **
     */
    @Test
    void floorEntry_nullKeyPresent_returnsStoredNullEntry() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        // sentinel for null key
        intMap.put(null, "nullValue");
        // when
        Entry<Integer, String> entry = intMap.floorEntry(null);
        // then
        assertNotNull(entry, "Entry should be returned for a stored null key");
        assertNull(entry.getKey(), "Key must be exposed as null");
        assertEquals("nullValue", entry.getValue());
    }

    /**
     * 2. floorEntry with exact existing key **
     */
    @Test
    void floorEntry_exactExistingKey_returnsCorrespondingEntry() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        // when
        Entry<Integer, String> entry = intMap.floorEntry(3);
        // then
        assertNotNull(entry);
        assertEquals(3, entry.getKey());
        assertEquals("three", entry.getValue());
    }

    /**
     * 3. floorEntry with non‑existing key that has a lower entry **
     */
    @Test
    void floorEntry_nonExistingKeyWithLowerEntry_returnsGreatestLower() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        // when
        Entry<Integer, String> entry = intMap.floorEntry(4);
        // then
        assertNotNull(entry);
        assertEquals(3, entry.getKey());
        assertEquals("three", entry.getValue());
    }

    /**
     * 4. floorEntry with key lower than any map key **
     */
    @Test
    void floorEntry_keyLowerThanAll_returnsNull() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        // when
        Entry<Integer, String> entry = intMap.floorEntry(0);
        // then
        assertNull(entry, "No entry should be returned when key is lower than every map entry");
    }

    /**
     * 5. floorEntry with key greater than the greatest map key **
     */
    @Test
    void floorEntry_keyGreaterThanGreatest_returnsLastEntry() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        // when
        Entry<Integer, String> entry = intMap.floorEntry(10);
        // then
        assertNotNull(entry);
        assertEquals(5, entry.getKey());
        assertEquals("five", entry.getValue());
    }

    /**
     * 6. floorEntry on an empty map **
     */
    @Test
    void floorEntry_onEmptyMap_returnsNull() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        // when
        Entry<Integer, String> entry = emptyMap.floorEntry(2);
        // then
        assertNull(entry, "Empty map must always return null");
    }

    /**
     * 7. floorEntry with key of incompatible type **
     */
    @Test
    void floorEntry_incompatibleKeyType_throwsClassCastException() {
        // given
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        // when / then
        assertThrows(ClassCastException.class, () -> intMap.floorEntry("stringKey"), "Supplying a key of the wrong type must propagate ClassCastException");
    }

    /**
     * 8. floorEntry with comparator throwing IllegalArgumentException **
     */
    @Test
    void floorEntry_comparatorThrowsIllegalArgumentException_propagates() throws Exception {
        // given – a comparator that rejects negative numbers
        Comparator<Integer> badComparator = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative keys not allowed");
            }
            return Integer.compare(a, b);
        };
        // The map does not expose a public constructor with a comparator in the provided source,
        // therefore we instantiate it and inject a custom internal map via reflection.
        ConcurrentNavigableMapNullSafe<Integer, String> mapWithBadComparator = new ConcurrentNavigableMapNullSafe<>();
        // Replace the internal ConcurrentSkipListMap with one that uses the bad comparator.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        ConcurrentSkipListMap<Integer, String> customMap = new ConcurrentSkipListMap<>(badComparator);
        customMap.put(1, "one");
        customMap.put(2, "two");
        internalMapField.set(mapWithBadComparator, customMap);
        // when / then – negative key triggers IllegalArgumentException from the comparator
        assertThrows(IllegalArgumentException.class, () -> mapWithBadComparator.floorEntry(-1), "Comparator‑driven IllegalArgumentException must propagate");
    }

    /**
     * 9. floorEntry when internal map is null **
     */
    @Test
    void floorEntry_internalMapNull_throwsNullPointerException() throws Exception {
        // given – a map with a null internalMap (set via reflection)
        ConcurrentNavigableMapNullSafe<Integer, String> mapWithNullInternal = new ConcurrentNavigableMapNullSafe<>();
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // force NPE scenario
        internalMapField.set(mapWithNullInternal, null);
        // when / then
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.floorEntry(1), "Calling floorEntry when internalMap is null must throw NullPointerException");
    }
}
