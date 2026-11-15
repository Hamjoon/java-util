package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

// retained from template; not strictly required but harmless
@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lowerKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not required but keeps the pattern consistent
        map = null;
    }

    private static Stream<Arguments> basicOrderingProvider() {
        return Stream.of(Arguments.of("b", "a"), Arguments.of("a", null), Arguments.of("d", "c"), Arguments.of("aa", "a"), Arguments.of("0", null));
    }

    private static Stream<Arguments> reverseComparatorProvider() {
        return Stream.of(Arguments.of("b", "c"), Arguments.of("a", "b"), Arguments.of("c", null), // null key: greatest key less than null is "a" in reverse order
        Arguments.of(null, "a"));
    }

    private static Stream<Arguments> integerKeyProvider() {
        return Stream.of(Arguments.of(0, -10), Arguments.of(-5, -10), Arguments.of(-10, null));
    }

    @Test
    void testLowerKeySimpleCase() {
        // Populate map with natural ordering keys
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // lowerKey should return the immediate predecessor
        assertEquals("a", map.lowerKey("b"));
        assertEquals("b", map.lowerKey("c"));
        // No key lower than the smallest element
        assertNull(map.lowerKey("a"));
    }

    @Test
    void testLowerKeyWhenKeyIsNull() {
        // Null keys are considered *last* in the ordering.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is allowed
        map.put(null, 99);
        // The greatest key strictly less than null should be the highest natural key
        assertEquals("c", map.lowerKey(null));
        // Also verify that lowerKey works for a normal key when a null key is present
        assertEquals("b", map.lowerKey("c"));
    }

    @Test
    void testLowerKeyWithCustomComparator() {
        // Comparator that reverses the natural order of strings
        Comparator<String> reverse = (s1, s2) -> s2.compareTo(s1);
        map = new ConcurrentNavigableMapNullSafe<>(reverse);
        // Insert keys; ordering will be reverse (c < b < a)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is still considered last
        map.put(null, 99);
        // In reverse order, the key lower than "b" is "c"
        assertEquals("c", map.lowerKey("b"));
        // Lower than "a" is "b"
        assertEquals("b", map.lowerKey("a"));
        // No key lower than the smallest according to the comparator ("c")
        assertNull(map.lowerKey("c"));
        // With a null key, the greatest key less than null is the *largest* according to the comparator,
        // which is "a" (because reverse order makes "a" the greatest)
        assertEquals("a", map.lowerKey(null));
    }

    @Test
    void testLowerKeyWhenMapIsEmpty() {
        // Empty map – any lowerKey call should return null
        assertNull(map.lowerKey("any"));
        assertNull(map.lowerKey(null));
    }

    /* --------------------------------------------------------------------- *
     *  BASIC ORDERING
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: lowerKey({0}) → {1}")
    @MethodSource("basicOrderingProvider")
    void testLowerKeyBasicOrdering(String key, String expected) {
        // map contains a, b, c
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(expected, map.lowerKey(key));
    }

    /* --------------------------------------------------------------------- *
     *  NULL HANDLING
     * --------------------------------------------------------------------- */
    @Test
    void testLowerKeyWhenKeyIsNullAndMapHasOnlyNonNullKeys() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // According to the JSON spec, lowerKey(null) should return null when no
        // explicit null key is present.
        assertNull(map.lowerKey(null));
    }

    @Test
    void testLowerKeyWhenMapContainsNullKey() {
        map.put(null, 99);
        map.put("a", 1);
        map.put("b", 2);
        // With a null key present, lowerKey("b") is expected to return the original null key.
        assertNull(map.lowerKey("b"));
    }

    /* --------------------------------------------------------------------- *
     *  CUSTOM COMPARATOR – REVERSE ORDER
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: lowerKey({0}) → {1}")
    @MethodSource("reverseComparatorProvider")
    void testLowerKeyWithReverseComparator(String key, String expected) {
        Comparator<String> reverse = (s1, s2) -> s2.compareTo(s1);
        map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is still last
        map.put(null, 99);
        assertEquals(expected, map.lowerKey(key));
    }

    /* --------------------------------------------------------------------- *
     *  INTEGER KEYS
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: lowerKey({0}) → {1}")
    @MethodSource("integerKeyProvider")
    void testLowerKeyWithIntegerKeys(Integer key, Integer expected) {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(-10, "neg10");
        intMap.put(0, "zero");
        intMap.put(10, "pos10");
        assertEquals(expected, intMap.lowerKey(key));
    }

    /* --------------------------------------------------------------------- *
     *  TYPE MISMATCH
     * --------------------------------------------------------------------- */
    @Test
    void testLowerKeyTypeMismatch() {
        map.put("a", 1);
        // The map is declared with String keys; passing an Integer must raise ClassCastException.
        assertThrows(ClassCastException.class, () -> map.lowerKey((String) (Object) 123));
    }

    /* --------------------------------------------------------------------- *
     *  COMPARATOR THAT THROWS EXCEPTION
     * --------------------------------------------------------------------- */
    @Test
    void testLowerKeyComparatorThrowsIllegalArgumentExceptionOnTrigger() {
        Comparator<String> throwingComparator = (o1, o2) -> {
            if ("trigger".equals(o1) || "trigger".equals(o2)) {
                throw new IllegalArgumentException("trigger key not allowed");
            }
            return o1.compareTo(o2);
        };
        map = new ConcurrentNavigableMapNullSafe<>(throwingComparator);
        map.put("a", 1);
        map.put("b", 2);
        assertThrows(IllegalArgumentException.class, () -> map.lowerKey("trigger"));
    }

    @Test
    void testLowerKeyComparatorThrowsIllegalArgumentExceptionOnAnyComparison() {
        // Comparator that unconditionally throws IllegalArgumentException.
        Comparator<String> alwaysThrow = (o1, o2) -> {
            throw new IllegalArgumentException("Comparator always fails");
        };
        map = new ConcurrentNavigableMapNullSafe<>(alwaysThrow);
        // masked null key will be compared during lowerKey
        map.put(null, 99);
        map.put("a", 1);
        map.put("b", 2);
        assertThrows(IllegalArgumentException.class, () -> map.lowerKey("any"));
    }

    /* --------------------------------------------------------------------- *
     *  COMPARATOR CANNOT COMPARE MASKED NULL (ClassCastException)
     * --------------------------------------------------------------------- */
    @Test
    void testLowerKeyComparatorCannotCompareMaskedNullWhenKeyIsNull() {
        // Comparator that casts arguments to String; the internal null‑sentinel is not a String.
        Comparator<Object> unsafeComparator = (o1, o2) -> ((String) o1).compareTo((String) o2);
        ConcurrentNavigableMapNullSafe<Object, Integer> unsafeMap = new ConcurrentNavigableMapNullSafe<>(unsafeComparator);
        unsafeMap.put(null, 1);
        unsafeMap.put("a", 2);
        assertThrows(ClassCastException.class, () -> unsafeMap.lowerKey(null));
    }

    @Test
    void testLowerKeyComparatorCannotCompareMaskedNullWhenKeyIsNormal() {
        Comparator<Object> unsafeComparator = (o1, o2) -> ((String) o1).compareTo((String) o2);
        ConcurrentNavigableMapNullSafe<Object, Integer> unsafeMap = new ConcurrentNavigableMapNullSafe<>(unsafeComparator);
        unsafeMap.put(null, 1);
        unsafeMap.put("a", 2);
        unsafeMap.put("b", 3);
        // lowerKey("b") forces the map to compare the masked null sentinel with "b"
        assertThrows(ClassCastException.class, () -> unsafeMap.lowerKey("b"));
    }

    /* --------------------------------------------------------------------- *
     *  MIXED KEY TYPES (ClassCastException)
     * --------------------------------------------------------------------- */
    @Test
    void testLowerKeyMixedKeyTypesStringArgument() {
        // Raw map that holds both Integer and String keys.
        ConcurrentNavigableMapNullSafe<Object, Integer> mixedMap = new ConcurrentNavigableMapNullSafe<>();
        mixedMap.put(1, 100);
        mixedMap.put("a", 200);
        assertThrows(ClassCastException.class, () -> mixedMap.lowerKey("z"));
    }

    @Test
    void testLowerKeyMixedKeyTypesIntegerArgument() {
        ConcurrentNavigableMapNullSafe<Object, Integer> mixedMap = new ConcurrentNavigableMapNullSafe<>();
        mixedMap.put(1, 100);
        mixedMap.put("a", 200);
        assertThrows(ClassCastException.class, () -> mixedMap.lowerKey(5));
    }
}
