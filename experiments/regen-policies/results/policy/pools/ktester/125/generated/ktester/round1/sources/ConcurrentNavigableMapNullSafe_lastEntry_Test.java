package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lastEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to guarantee isolation
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no leftover state leaks between tests
        map = null;
    }

    @Test
    void lastEntry_returnsHighestEntry_whenMapIsNonEmptyAndNoNullKey() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);
        // when
        Entry<String, Integer> last = map.lastEntry();
        // then
        assertNotNull(last, "lastEntry() should not return null for a non‑empty map");
        assertEquals("c", last.getKey(), "The highest key according to natural ordering should be returned");
        assertEquals(3, last.getValue());
    }

    @Test
    void lastEntry_returnsNullKeyEntry_whenNullKeyIsPresent() {
        // given
        map.put("x", 10);
        map.put("y", 20);
        // null is considered the greatest key (per original test)
        map.put(null, 99);
        // when
        Entry<String, Integer> last = map.lastEntry();
        // then
        assertNotNull(last, "lastEntry() should return the entry for the null key when present");
        assertNull(last.getKey(), "The key of the returned entry must be null");
        assertEquals(99, last.getValue());
    }

    @Test
    void lastEntry_returnsNull_whenMapIsEmpty() {
        // given – an empty map (setupBeforeEach already provides this)
        // when
        Entry<String, Integer> last = map.lastEntry();
        // then
        assertNull(last, "lastEntry() must return null for an empty map");
    }

    @Test
    void lastEntry_reflectsUpdatesAfterRetrieval() {
        // given
        map.put("alpha", 1);
        map.put("beta", 2);
        // should be "beta"
        Entry<String, Integer> last = map.lastEntry();
        // when – mutate the map
        // new highest key
        map.put("gamma", 3);
        // remove previously highest
        map.remove("beta");
        // then – the previously obtained entry should still represent the original mapping
        assertEquals("beta", last.getKey());
        assertEquals(2, last.getValue());
        // and a fresh call reflects the new state
        Entry<String, Integer> newLast = map.lastEntry();
        assertEquals("gamma", newLast.getKey());
        assertEquals(3, newLast.getValue());
    }

    @Test
    void lastEntry_singleElement() {
        // Map contains a single entry (1 -> "one")
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(1, last.getKey());
        assertEquals("one", last.getValue());
    }

    @Test
    void lastEntry_multipleElements_naturalOrdering() {
        // (1 -> "one"), (2 -> "two"), (3 -> "three")
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(3, last.getKey());
        assertEquals("three", last.getValue());
    }

    @Test
    void lastEntry_nullKeyIsLowest_maskUnmaskBehaviour() {
        // null is treated as the lowest key, therefore lastEntry should return the normal key (5)
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, "nullKey");
        intMap.put(5, "five");
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(5, last.getKey());
        assertEquals("five", last.getValue());
    }

    @Test
    void lastEntry_onlyNullKey() {
        // Map contains only a null key
        ConcurrentNavigableMapNullSafe<String, String> strMap = new ConcurrentNavigableMapNullSafe<>();
        strMap.put(null, "nullOnly");
        Entry<String, String> last = strMap.lastEntry();
        assertNotNull(last);
        assertNull(last.getKey());
        assertEquals("nullOnly", last.getValue());
    }

    @Test
    void lastEntry_afterPollLastEntry() {
        // (1 -> "one"), (2 -> "two"), (3 -> "three")
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // remove the highest entry
        Entry<Integer, String> polled = intMap.pollLastEntry();
        assertNotNull(polled);
        assertEquals(3, polled.getKey());
        // now lastEntry should be 2
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(2, last.getKey());
        assertEquals("two", last.getValue());
    }

    @Test
    void lastEntry_valueNullHandling() {
        // (1 -> null) and (2 -> "two")
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, null);
        intMap.put(2, "two");
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(2, last.getKey());
        assertEquals("two", last.getValue());
    }

    @Test
    void lastEntry_customComparator_reverseOrder() {
        // Comparator that orders keys in reverse (largest natural key becomes first)
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        intMap.put(1, "one");
        intMap.put(2, "two");
        // In reverse order, the "last" entry is the smallest natural key (1)
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(1, last.getKey());
        assertEquals("one", last.getValue());
    }

    @Test
    void lastEntry_customComparator_absoluteValueOrdering() {
        // Comparator that orders by absolute value ascending
        Comparator<Integer> absComparator = Comparator.comparingInt(Math::abs);
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(absComparator);
        intMap.put(-5, "minusFive");
        intMap.put(3, "three");
        intMap.put(-2, "minusTwo");
        // The entry with the greatest absolute value is -5
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(-5, last.getKey());
        assertEquals("minusFive", last.getValue());
    }

    @Test
    void lastEntry_afterClear() {
        // Populate then clear
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.clear();
        Entry<Integer, String> last = intMap.lastEntry();
        assertNull(last, "After clear() the map is empty, so lastEntry() must return null");
    }

    @Test
    void lastEntry_maxIntegerKey() {
        // (0 -> "zero"), (100 -> "hundred"), (Integer.MAX_VALUE -> "maxInt")
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(0, "zero");
        intMap.put(100, "hundred");
        intMap.put(Integer.MAX_VALUE, "maxInt");
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(Integer.MAX_VALUE, last.getKey());
        assertEquals("maxInt", last.getValue());
    }

    @Test
    void lastEntry_negativeKeysNaturalOrdering() {
        // (-10 -> "minusTen"), (-5 -> "minusFive"), (0 -> "zero")
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(-10, "minusTen");
        intMap.put(-5, "minusFive");
        intMap.put(0, "zero");
        Entry<Integer, String> last = intMap.lastEntry();
        assertNotNull(last);
        assertEquals(0, last.getKey());
        assertEquals("zero", last.getValue());
    }

    @Test
    void lastEntry_internalMapNull_throwsNPE() throws Exception {
        // Force internalMap to null via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.lastEntry(), "When internalMap is null, lastEntry() should throw NullPointerException");
    }

    @Test
    void lastEntry_internalMapWrongType_throwsClassCastException() throws Exception {
        // Replace internalMap with a plain HashMap (not a ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.lastEntry(), "Casting a non‑ConcurrentSkipListMap internalMap should result in ClassCastException");
    }

    @Test
    void lastEntry_comparatorThrowsDuringOrdering_propagatesException() {
        // Comparator that always throws
        Comparator<Integer> explodingComparator = (a, b) -> {
            throw new IllegalStateException("Comparator exploded");
        };
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(explodingComparator);
        intMap.put(1, "one");
        intMap.put(2, "two");
        IllegalStateException ex = assertThrows(IllegalStateException.class, intMap::lastEntry, "Exception thrown by comparator must propagate");
        assertEquals("Comparator exploded", ex.getMessage());
    }

    @Test
    void lastEntry_nonComparableKeyWithoutComparator_throwsClassCastException() {
        // Map without explicit comparator; insert a non‑Comparable key
        ConcurrentNavigableMapNullSafe<Object, String> objMap = new ConcurrentNavigableMapNullSafe<>();
        Object nonComparableKey = new Object();
        objMap.put(nonComparableKey, "value");
        assertThrows(ClassCastException.class, objMap::lastEntry, "Non‑Comparable key without a comparator should cause ClassCastException");
    }
}
