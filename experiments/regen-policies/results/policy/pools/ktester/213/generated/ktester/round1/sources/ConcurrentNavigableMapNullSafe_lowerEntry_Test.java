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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lowerEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a known ordering
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Null key is considered last in ascending order
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC
        map = null;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// exact match
        Arguments.of(2, new AbstractMap.SimpleEntry<>(1, "a")), // between keys
        Arguments.of(2.5, new AbstractMap.SimpleEntry<>(2, "b")), // smallest key
        Arguments.of(1, null));
    }

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(// greater than only key
        Arguments.of(20, new AbstractMap.SimpleEntry<>(10, "x")), // equal to only key
        Arguments.of(10, null), // less than only key
        Arguments.of(5, null));
    }

    @Test
    void lowerEntryWithExistingKey() {
        // lowerEntry returns the greatest entry strictly less than the given key
        Map.Entry<String, Integer> entry = map.lowerEntry("b");
        assertNotNull(entry);
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
    }

    @Test
    void lowerEntryWithFirstKeyReturnsNull() {
        // There is no key less than the smallest key ("a")
        assertNull(map.lowerEntry("a"));
    }

    @Test
    void lowerEntryWithNullKeyReturnsGreatestNonNullKey() {
        // Null is considered the greatest key, so the entry lower than null
        // should be the greatest non‑null key, i.e., "c"
        Map.Entry<String, Integer> entry = map.lowerEntry(null);
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void lowerEntryWithKeyNotPresentButBetweenExistingKeys() {
        // Key "bb" lies between "b" and "c"
        Map.Entry<String, Integer> entry = map.lowerEntry("bb");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
    }

    @Test
    void lowerEntryWithKeyLessThanAllExistingReturnsNull() {
        // Any key that sorts before the smallest key should yield null
        assertNull(map.lowerEntry(""));
    }

    @Test
    void lowerEntryDoesNotModifyMap() {
        // Capture size before call
        int sizeBefore = map.size();
        map.lowerEntry("b");
        assertEquals(sizeBefore, map.size(), "lowerEntry must not alter the map");
    }

    /* --------------------------------------------------------------
     *  Basic functionality – numeric keys (group: lowerEntry basic functionality)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: lowerEntry({0}) → {1}")
    @MethodSource("basicFunctionalityProvider")
    void lowerEntryBasicFunctionality(Number key, Map.Entry<Integer, String> expected) {
        // Build a numeric map for this test
        ConcurrentNavigableMapNullSafe<Integer, String> numericMap = new ConcurrentNavigableMapNullSafe<>();
        numericMap.put(1, "a");
        numericMap.put(2, "b");
        numericMap.put(3, "c");
        Map.Entry<Integer, String> result = numericMap.lowerEntry(key.intValue());
        if (expected == null) {
            assertNull(result, "Expected null for key " + key);
        } else {
            assertNotNull(result);
            assertEquals(expected.getKey(), result.getKey());
            assertEquals(expected.getValue(), result.getValue());
        }
    }

    /* --------------------------------------------------------------
     *  Empty map (group: lowerEntry with empty map)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryEmptyMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        assertNull(emptyMap.lowerEntry(42), "lowerEntry on an empty map must return null");
    }

    /* --------------------------------------------------------------
     *  Single element map (group: lowerEntry with single element map)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: lowerEntry({0}) on single‑element map → {1}")
    @MethodSource("singleElementProvider")
    void lowerEntrySingleElement(Number key, Map.Entry<Integer, String> expected) {
        ConcurrentNavigableMapNullSafe<Integer, String> singleMap = new ConcurrentNavigableMapNullSafe<>();
        singleMap.put(10, "x");
        Map.Entry<Integer, String> result = singleMap.lowerEntry(key.intValue());
        if (expected == null) {
            assertNull(result);
        } else {
            assertNotNull(result);
            assertEquals(expected.getKey(), result.getKey());
            assertEquals(expected.getValue(), result.getValue());
        }
    }

    /* --------------------------------------------------------------
     *  Key greater than max key (group: lowerEntry with key greater than max key)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryKeyGreaterThanMax() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "a");
        map.put(3, "c");
        map.put(5, "e");
        Map.Entry<Integer, String> result = map.lowerEntry(6);
        assertNotNull(result);
        assertEquals(5, result.getKey());
        assertEquals("e", result.getValue());
    }

    /* --------------------------------------------------------------
     *  Null‑key handling (group: lowerEntry with null key handling)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryNullKeyReturnsNullWhenNullIsPresent() {
        // Map contains a null key and a regular key 2.
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, "nullVal");
        map.put(2, "b");
        // According to the specification used in the test data, lowerEntry(null) should be null.
        assertNull(map.lowerEntry(null), "lowerEntry(null) should return null when null key is present");
    }

    @Test
    void lowerEntryWithNonNullKeyReturnsNullKeyEntry() {
        // Same map as above – the null key is considered the smallest after masking,
        // therefore it is the entry lower than any non‑null key.
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, "nullVal");
        map.put(2, "b");
        Map.Entry<Integer, String> result = map.lowerEntry(2);
        assertNotNull(result);
        assertNull(result.getKey(), "The returned entry should have a null key");
        assertEquals("nullVal", result.getValue());
    }

    /* --------------------------------------------------------------
     *  Custom comparator – reverse order (group: lowerEntry with custom comparator (reverse order))
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryWithReverseOrderComparator() {
        // Comparator that orders integers in reverse (largest first)
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        // In reverse order the keys are 3,2,1 – the greatest key strictly less than 2 is 3.
        Map.Entry<Integer, String> result = map.lowerEntry(2);
        assertNotNull(result);
        assertEquals(3, result.getKey());
        assertEquals("c", result.getValue());
    }

    /* --------------------------------------------------------------
     *  Entry mutability (group: lowerEntry entry mutability)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryReturnedEntryIsMutable() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        Map.Entry<Integer, String> entry = map.lowerEntry(2);
        assertNotNull(entry);
        assertEquals(1, entry.getKey());
        assertEquals("a", entry.getValue());
        // Mutate via setValue and verify the underlying map reflects the change
        entry.setValue("newA");
        assertEquals("newA", map.get(1));
        assertEquals("newA", entry.getValue());
    }

    /* --------------------------------------------------------------
     *  Incompatible key type (group: lowerEntry with incompatible key type)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryIncompatibleKeyTypeThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        // Supplying a String where Integer is expected should cause ClassCastException
        assertThrows(ClassCastException.class, () -> intMap.lowerEntry((Integer) (Object) "stringKey"));
    }

    /* --------------------------------------------------------------
     *  Comparator that throws an exception (group: lowerEntry with comparator throwing exception)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryComparatorThrowsExceptionPropagates() {
        Comparator<Integer> badComparator = (o1, o2) -> {
            throw new IllegalStateException("Comparator failure");
        };
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(badComparator);
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.lowerEntry(5));
        assertEquals("Comparator failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Internal map is null (group: lowerEntry when internal map is null)
     * -------------------------------------------------------------- */
    @Test
    void lowerEntryWhenInternalMapIsNullThrowsNullPointerException() throws Exception {
        // Use reflection to set the protected internalMap field to null
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // The call should now throw NPE because the delegate map is missing
        assertThrows(NullPointerException.class, () -> map.lowerEntry("any"));
    }
}
