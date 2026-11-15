package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_higherKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private ConcurrentNavigableMapNullSafe<String, Integer> mapString;

    private ConcurrentNavigableMapNullSafe<Integer, Integer> mapInteger;

    /* -------------------------------------------------------------
     *  Comparator that throws RuntimeException
     * ------------------------------------------------------------- */
    private static class ExplodingComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if ("trigger".equals(o1) || "trigger".equals(o2)) {
                throw new RuntimeException("Comparator triggered an exception");
            }
            return o1.compareTo(o2);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh maps for each test method
        mapString = new ConcurrentNavigableMapNullSafe<>();
        mapInteger = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; the map itself does not hold external resources
        map = null;
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the first group: null handling & ordering
     * ------------------------------------------------------------- */
    private static Stream<Arguments> nullHandlingAndOrderingProvider() {
        return Stream.of(// key = null -> expected null (no higher key)
        Arguments.of(null, null, "When the input key is null, there is no higher key."), // key = "a" -> expected "b"
        Arguments.of("a", "b", "Typical case: higherKey('a') should be 'b'."), // key = "c" -> expected null (greatest element)
        Arguments.of("c", null, "When the given key is the greatest element, higherKey should return null."), // key = "z" -> expected null (greater than any existing key)
        Arguments.of("z", null, "Key not present and greater than any existing key; higherKey returns null."), // key = "0" -> expected "a" (less than smallest element)
        Arguments.of("0", "a", "Key less than the smallest element; higherKey returns the smallest key."));
    }

    @Test
    void testHigherKeyBasicOrdering() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // when / then
        assertEquals("b", map.higherKey("a"), "higherKey('a') should be 'b'");
        assertEquals("c", map.higherKey("b"), "higherKey('b') should be 'c'");
        assertNull(map.higherKey("c"), "higherKey('c') should be null because 'c' is the greatest non‑null key");
    }

    @Test
    void testHigherKeyWhenNullKeyIsPresent() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        // null is considered the greatest key
        map.put(null, 99);
        // when / then
        assertEquals(null, map.higherKey("b"), "higherKey('b') should be null because null key is greater than any non‑null key");
        assertNull(map.higherKey(null), "higherKey(null) should be null because null is already the greatest key");
    }

    @Test
    void testHigherKeyWithCustomComparator() {
        // given a map that orders keys in reverse natural order
        map = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is still treated as the last element
        map.put(null, 0);
        // when / then
        // In reverse order: c < b < a (according to the map's ordering)
        assertEquals("a", map.higherKey("b"), "With reverse order, higherKey('b') should be 'a'");
        assertEquals("b", map.higherKey("c"), "With reverse order, higherKey('c') should be 'b'");
        assertEquals(null, map.higherKey("a"), "With reverse order, higherKey('a') should be null (null key is last)");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullHandlingAndOrderingProvider")
    void testHigherKeyNullHandlingAndOrdering(String key, String expected, String description) {
        // given – a map with three natural‑order string keys
        mapString.put("a", 1);
        mapString.put("b", 2);
        mapString.put("c", 3);
        // when
        String result = mapString.higherKey(key);
        // then
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Non‑existent intermediate key
     * ------------------------------------------------------------- */
    @Test
    void testHigherKeyNonExistentIntermediateKey() {
        // given a map with gaps: ["a","c","e"]
        mapString.put("a", 1);
        mapString.put("c", 3);
        mapString.put("e", 5);
        // when
        String result = mapString.higherKey("b");
        // then
        assertEquals("c", result, "Map contains [\"a\",\"c\",\"e\"]. The key \"b\" lies between \"a\" and \"c\"; higherKey should return \"c\".");
    }

    /* -------------------------------------------------------------
     *  Null key with other entries
     * ------------------------------------------------------------- */
    @Test
    void testHigherKeyNullKeyWithOtherEntries() {
        // given a map that contains a null key and a regular key "a"
        mapString.put(null, 99);
        mapString.put("a", 1);
        // when
        String result = mapString.higherKey(null);
        // then
        // According to the class contract, null is the greatest key, therefore
        // there is no key greater than null. The expected behaviour is null.
        assertNull(result, "When null is present and considered the greatest key, higherKey(null) should be null.");
    }

    /* -------------------------------------------------------------
     *  Only null key present
     * ------------------------------------------------------------- */
    @Test
    void testHigherKeyOnlyNullKeyPresent() {
        // given a map that contains only a null key
        mapString.put(null, 42);
        // when
        String result = mapString.higherKey(null);
        // then
        assertNull(result, "With only a null key present, higherKey(null) must return null because there is no greater key.");
    }

    /* -------------------------------------------------------------
     *  Incompatible key type – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void testHigherKeyIncompatibleKeyType() {
        // given a map with String keys only
        mapString.put("a", 1);
        mapString.put("b", 2);
        // when / then
        assertThrows(ClassCastException.class, () -> mapString.higherKey((String) (Object) Integer.valueOf(123)), "Supplying an Integer key to a map of String keys should throw ClassCastException.");
    }

    /* -------------------------------------------------------------
     *  Integer key boundaries
     * ------------------------------------------------------------- */
    @Test
    void testHigherKeyBoundaryWithSmallestIntegerKey() {
        // given integer keys [-10, -5, 0, 5]
        mapInteger.put(-10, 1);
        mapInteger.put(-5, 2);
        mapInteger.put(0, 3);
        mapInteger.put(5, 4);
        // when
        Integer result = mapInteger.higherKey(-10);
        // then
        assertEquals(Integer.valueOf(-5), result, "higherKey(-10) should return the next greater integer key -5.");
    }

    @Test
    void testHigherKeyKeyGreaterThanAllIntegerKeys() {
        // given integer keys [0, 10, 20]
        mapInteger.put(0, 1);
        mapInteger.put(10, 2);
        mapInteger.put(20, 3);
        // when
        Integer result = mapInteger.higherKey(100);
        // then
        assertNull(result, "higherKey(100) should be null because 100 exceeds the greatest key in the map.");
    }

    @Test
    void testHigherKeyComparatorThrowsRuntimeException() {
        // given a map with a comparator that throws on the value "trigger"
        ConcurrentNavigableMapNullSafe<String, Integer> explodingMap = new ConcurrentNavigableMapNullSafe<>(new ExplodingComparator());
        explodingMap.put("alpha", 1);
        explodingMap.put("beta", 2);
        // entry that will cause the comparator to explode
        explodingMap.put("trigger", 3);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> explodingMap.higherKey("trigger"), "Calling higherKey(\"trigger\") should propagate the RuntimeException from the comparator.");
        assertEquals("Comparator triggered an exception", ex.getMessage());
    }
}
