package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_ceilingKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    // used for numeric tests
    private ConcurrentNavigableMapNullSafe<Integer, String> intMap;

    // used for string‑key tests
    private ConcurrentNavigableMapNullSafe<String, Integer> strMap;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh maps for each test method.
        intMap = new ConcurrentNavigableMapNullSafe<>();
        strMap = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help the garbage collector; not strictly necessary.
        map.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        // map will contain keys [1,2,3]
        return Stream.of(Arguments.of(1, 1, "When the map contains keys [1,2,3] and key=1, ceilingKey should return the same key."), Arguments.of(2, 2, "When the map contains keys [1,2,3] and key=2, ceilingKey should return 2."), Arguments.of(0, 1, "When the map contains keys [1,2,3] and key=0 (less than smallest), ceilingKey returns the smallest key."), Arguments.of(4, null, "When the map contains keys [1,2,3] and key=4 (greater than largest), ceilingKey returns null."));
    }

    private static Stream<Arguments> gapsProvider() {
        return Stream.of(Arguments.of(4, 5, "Map contains keys [1,3,5]; key=4 lies between 3 and 5, so ceilingKey should return the next higher key 5."), Arguments.of(-2, 1, "Map contains keys [1,3,5]; key=-2 is less than the smallest key, so ceilingKey returns the smallest key 1."));
    }

    private static Stream<Arguments> reverseComparatorProvider() {
        return Stream.of(Arguments.of(2, 2, "Using a descending comparator, map keys are ordered [3,2,1]; for key=2 the ceiling (least key ≥ 2 in this order) is 2."), Arguments.of(0, 3, "With descending order, the smallest key that is ≥ 0 is the greatest natural key, 3."), Arguments.of(4, null, "In descending order, there is no key ≥ 4, so ceilingKey returns null."));
    }

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(10, 10, "Map contains a single key 10; ceilingKey for the exact key returns 10."), Arguments.of(5, 10, "Map contains a single key 10; ceilingKey for a smaller key returns the only key 10."), Arguments.of(15, null, "Map contains a single key 10; ceilingKey for a larger key returns null."));
    }

    @Test
    void ceilingKeyWithNormalKeys() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // when / then
        assertEquals("a", map.ceilingKey("a"), "Exact match should return the same key");
        assertEquals("c", map.ceilingKey("b"), "Key greater than 'b' is 'c'");
        assertEquals("c", map.ceilingKey("c"), "Exact match should return the same key");
        assertEquals("e", map.ceilingKey("d"), "Key greater than 'd' is 'e'");
        assertEquals("e", map.ceilingKey("e"), "Exact match should return the same key");
        assertNull(map.ceilingKey("f"), "No key >= 'f' should return null");
    }

    @Test
    void ceilingKeyWhenMapContainsNullKey() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        // null is considered the greatest key
        map.put(null, 99);
        // when / then
        assertEquals("a", map.ceilingKey("a"));
        assertEquals("c", map.ceilingKey("b"));
        // The null key is the greatest; any key less than null should ceiling to the next
        // real key, unless that key is null itself.
        assertEquals(null, map.ceilingKey(null), "Ceiling of null key should be the null key itself");
        // A key greater than any real key but less than null should still ceiling to null.
        assertEquals(null, map.ceilingKey("z"));
    }

    @Test
    void ceilingKeyOnEmptyMapReturnsNull() {
        // given: map is empty (setupBeforeEach already created an empty map)
        // when / then
        assertNull(map.ceilingKey("any"), "Empty map must return null for any key");
        assertNull(map.ceilingKey(null), "Empty map must return null for null key");
    }

    @Test
    void ceilingKeyWithComparator() {
        // given a map with a custom comparator that orders strings in reverse order.
        Comparator<String> reverse = Comparator.reverseOrder();
        map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put("a", 1);
        map.put("c", 3);
        // null is still considered last (greatest) even with reverse order
        map.put(null, 99);
        // In reverse order, the "least" key greater than or equal to "b" is "a"
        assertEquals("a", map.ceilingKey("b"));
        // For a key that would be after all real keys in reverse order, ceiling is null.
        assertEquals(null, map.ceilingKey("z"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void ceilingKeyBasicFunctionality(Integer key, Integer expected, String description) {
        // given
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // when
        Integer result = intMap.ceilingKey(key);
        // then
        assertEquals(expected, result);
    }

    @Test
    void ceilingKeyNullOnEmptyMap() {
        // description: When the map is empty and the supplied key is null, ceilingKey should return null.
        assertNull(intMap.ceilingKey(null));
    }

    @Test
    void ceilingKeyNullWhenMapContainsNullKey() {
        // description: When the map contains a null key (masked internally) and the supplied key is null,
        //              ceilingKey should return the null key.
        intMap.put(null, "nullValue");
        intMap.put(1, "one");
        intMap.put(2, "two");
        // null is stored as a special sentinel, unmasked back to null
        assertNull(intMap.ceilingKey(null));
    }

    @Test
    void ceilingKeyNonNullWithNullKeyPresent() {
        // description: Map contains keys [null, 1, 2]; for a non‑null key smaller than the smallest non‑null entry,
        //              ceilingKey should return the smallest non‑null key (1).
        intMap.put(null, "nullValue");
        intMap.put(1, "one");
        intMap.put(2, "two");
        assertEquals(1, intMap.ceilingKey(0));
    }

    @Test
    void ceilingKeyNonNullWhenOnlyNullKeyPresent() {
        // description: Map contains only a null key; any non‑null key larger than the null sentinel has no ceiling,
        //              so ceilingKey returns null.
        intMap.put(null, "nullValue");
        assertNull(intMap.ceilingKey(5));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("gapsProvider")
    void ceilingKeyGapsBetweenKeys(Integer key, Integer expected, String description) {
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        assertEquals(expected, intMap.ceilingKey(key));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("reverseComparatorProvider")
    void ceilingKeyWithReverseComparator(Integer key, Integer expected, String description) {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        intMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        assertEquals(expected, intMap.ceilingKey(key));
    }

    @Test
    void ceilingKeyTypeIncompatibility() {
        // description: Map is typed with Integer keys; supplying a String key causes a ClassCastException.
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "one");
        map.put(2, "two");
        assertThrows(ClassCastException.class, () -> map.ceilingKey((Integer) (Object) "stringKey"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("singleElementProvider")
    void ceilingKeySingleElementMap(Integer key, Integer expected, String description) {
        intMap.put(10, "ten");
        assertEquals(expected, intMap.ceilingKey(key));
    }

    @Test
    void ceilingKeyComparatorRuntimeExceptionOnKey() {
        // description: A custom comparator throws a RuntimeException when comparing the supplied key (5).
        Comparator<Integer> badComparator = (a, b) -> {
            if (a == null || b == null) {
                throw new RuntimeException("Comparator received null sentinel");
            }
            if (a == 5 || b == 5) {
                throw new RuntimeException("Comparator forced failure for key 5");
            }
            return Integer.compare(a, b);
        };
        intMap = new ConcurrentNavigableMapNullSafe<>(badComparator);
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        assertThrows(RuntimeException.class, () -> intMap.ceilingKey(5));
    }

    @Test
    void ceilingKeyComparatorRuntimeExceptionOnNullSentinel() {
        // description: Comparator throws when it receives the internal null‑mask sentinel.
        Comparator<Integer> badComparator = (a, b) -> {
            if (a == null || b == null) {
                throw new RuntimeException("Comparator received null sentinel");
            }
            return Integer.compare(a, b);
        };
        intMap = new ConcurrentNavigableMapNullSafe<>(badComparator);
        intMap.put(null, "nullValue");
        intMap.put(1, "one");
        assertThrows(RuntimeException.class, () -> intMap.ceilingKey(null));
    }
}
