package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lastKey_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Helper class for non‑Comparable key tests.
     */
    static class Obj {

        final int id;

        Obj(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Obj{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor uses natural ordering.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to guarantee isolation between tests.
        map.clear();
    }

    /**
     * Parameter source for natural ordering tests (a, b, c).
     */
    static List<Arguments> naturalOrderingProvider() {
        return List.of(Arguments.of(List.of("a", "b", "c"), "c"), Arguments.of(List.of("alpha", "beta", "gamma", "delta"), "gamma"));
    }

    @Test
    void lastKey_onEmptyMap_throwsNoSuchElementException() {
        // GIVEN an empty map
        // WHEN lastKey() is invoked
        // THEN a NoSuchElementException is thrown (behaviour mirrors ConcurrentSkipListMap)
        assertThrows(NoSuchElementException.class, () -> map.lastKey(), "lastKey() should throw NoSuchElementException when the map is empty");
    }

    @Test
    void lastKey_withoutNullKey_returnsGreatestKey() {
        // GIVEN a map with several non‑null keys
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        // WHEN lastKey() is called
        String last = map.lastKey();
        // THEN the greatest key according to natural ordering is returned
        assertEquals("cherry", last, "The last key should be the greatest (natural order) key");
    }

    @Test
    void lastKey_withNullKey_returnsNullAsLast() {
        // GIVEN a map that contains a null key and other keys
        map.put("alpha", 10);
        map.put("beta", 20);
        // null is considered the last key
        map.put(null, 999);
        // WHEN lastKey() is called
        String last = map.lastKey();
        // THEN null is returned because null keys are defined to be last
        assertNull(last, "When a null key is present, lastKey() must return null");
    }

    @Test
    void lastKey_withOnlyNullKey_returnsNull() {
        // GIVEN a map that contains only a null key
        map.put(null, 42);
        // WHEN lastKey() is called
        String last = map.lastKey();
        // THEN null is returned
        assertNull(last, "If the map contains only a null key, lastKey() should return null");
    }

    @Test
    void lastKey_afterRemovingNullKey_returnsGreatestNonNullKey() {
        // GIVEN a map with a null key and other keys
        map.put("x", 1);
        map.put("y", 2);
        map.put(null, 3);
        // WHEN the null key is removed
        map.remove(null);
        String last = map.lastKey();
        // THEN the greatest remaining key is returned
        assertEquals("y", last, "After removing the null key, lastKey() should return the greatest non‑null key");
    }

    @Test
    void lastKey_withCustomComparator_nullRemainsLast() {
        // GIVEN a map constructed with a reverse order comparator
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        // null should still be considered last irrespective of comparator
        reverseMap.put(null, 99);
        // WHEN lastKey() is called
        String last = reverseMap.lastKey();
        // THEN null is returned because null keys are always last by contract
        assertNull(last, "Null keys must be last even when a custom comparator is supplied");
    }

    @Test
    void lastKey_consistencyWithFirstKeyInDescendingMap() {
        // GIVEN a map with several keys including null
        map.put("first", 1);
        map.put("second", 2);
        map.put(null, 3);
        // WHEN we obtain the descending map and query its firstKey()
        ConcurrentNavigableMap<String, Integer> descending = map.descendingMap();
        String firstInDesc = descending.firstKey();
        // THEN the first key of the descending view must be the same as lastKey() of the original map
        assertEquals(map.lastKey(), firstInDesc, "firstKey() of the descending map should equal lastKey() of the original map");
    }

    @ParameterizedTest(name = "lastKey on natural order keys {0} should return {1}")
    @MethodSource("naturalOrderingProvider")
    void lastKey_naturalOrdering_returnsGreatestKey(List<String> keys, String expected) {
        // GIVEN a map populated with the supplied keys (values are irrelevant)
        keys.forEach(k -> map.put(k, 1));
        // WHEN lastKey() is called
        String actual = map.lastKey();
        // THEN the expected greatest key is returned
        assertEquals(expected, actual);
    }

    @Test
    void lastKey_nullKeyWithOtherKeys_returnsHighestRealKey() {
        // GIVEN a map that contains a null key and other regular keys.
        // According to the JSON description, the masking sentinel sorts lower than any real key,
        // therefore lastKey() should return the highest real key ("z").
        map.put("a", 1);
        map.put("m", 2);
        map.put("z", 3);
        map.put(null, 99);
        // WHEN lastKey() is called
        String last = map.lastKey();
        // THEN the highest real key is returned (null is masked to a lower sentinel)
        assertEquals("z", last, "When null masks to a lower sentinel, lastKey() must return the highest real key");
    }

    @Test
    void lastKey_singleNonNullKey_returnsThatKey() {
        // This test uses Integer keys to match the JSON expectation of key "42".
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(42, "the answer");
        // WHEN lastKey() is called
        Integer last = intMap.lastKey();
        // THEN the sole key is returned
        assertEquals(42, last);
    }

    @Test
    void lastKey_customComparatorDescendingOrder_returnsGreatestAccordingToComparator() {
        // GIVEN a map with a reverse order comparator (so "a" > "b" > "c")
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put("a", 1);
        revMap.put("b", 2);
        revMap.put("c", 3);
        // WHEN lastKey() is called
        String last = revMap.lastKey();
        // THEN "a" is considered the greatest under the reverse comparator
        assertEquals("a", last);
    }

    @Test
    void lastKey_customComparatorWithNullKey_returnsNull() {
        // GIVEN a reverse order comparator and a null key
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put("b", 1);
        revMap.put("c", 2);
        // null is masked and should be considered the greatest
        revMap.put(null, 99);
        // WHEN lastKey() is called
        String last = revMap.lastKey();
        // THEN null is returned after unmasking
        assertNull(last);
    }

    @Test
    void lastKey_integerMaxValue_returnsMaxValue() {
        // GIVEN a map with several Integer keys, including Integer.MAX_VALUE
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(10, "ten");
        intMap.put(Integer.MAX_VALUE, "max");
        intMap.put(0, "zero");
        // WHEN lastKey() is called
        Integer last = intMap.lastKey();
        // THEN the maximum integer key is returned
        assertEquals(Integer.MAX_VALUE, last);
    }

    @Test
    void lastKey_nonComparableKeysWithExplicitComparator_returnsGreatestByComparator() {
        // Comparator that orders Obj by their id field
        Comparator<Obj> byId = Comparator.comparingInt(o -> o.id);
        ConcurrentNavigableMapNullSafe<Obj, String> objMap = new ConcurrentNavigableMapNullSafe<>(byId);
        Obj obj1 = new Obj(1);
        Obj obj2 = new Obj(2);
        objMap.put(obj1, "first");
        objMap.put(obj2, "second");
        // WHEN lastKey() is called
        Obj last = objMap.lastKey();
        // THEN the object with the higher id (obj2) is returned
        assertSame(obj2, last);
    }

    @Test
    void lastKey_internalMapIsNull_throwsNullPointerException() throws Exception {
        // Use reflection to set the protected internalMap field to null
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // WHEN lastKey() is invoked
        // THEN a NullPointerException is expected because the casted map is null
        assertThrows(NullPointerException.class, () -> map.lastKey(), "If internalMap is null, lastKey() should throw NullPointerException");
    }

    @Test
    void lastKey_internalMapWrongType_throwsClassCastException() throws Exception {
        // Replace internalMap with a HashMap (which is not a ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new ConcurrentHashMap<>());
        // WHEN lastKey() is invoked
        // THEN a ClassCastException should be thrown due to the cast in lastKey()
        assertThrows(ClassCastException.class, () -> map.lastKey(), "If internalMap is not a ConcurrentSkipListMap, lastKey() must throw ClassCastException");
    }

    @Test
    void lastKey_nonComparableKeyWithNaturalOrdering_throwsClassCastException() throws Exception {
        // Insert a non‑Comparable key directly into the underlying ConcurrentSkipListMap via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentSkipListMap<Object, Object> underlying = (ConcurrentSkipListMap<Object, Object>) internalMapField.get(map);
        // A simple non‑Comparable key
        Object nonComparableKey = new Object();
        underlying.put(nonComparableKey, "value");
        // WHEN lastKey() is called
        // THEN the map's internal ordering logic will attempt to compare the key and fail
        assertThrows(ClassCastException.class, () -> map.lastKey(), "A non‑Comparable key in a naturally ordered map should cause ClassCastException on lastKey()");
    }
}
