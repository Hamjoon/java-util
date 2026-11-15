package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMapNullSafe;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.stream.Stream;

class ConcurrentNavigableMapNullSafe_ceilingKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh map instance for each test method.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        map = null;
    }

    private static Stream<Arguments> provideCeilingKeyBasicCases() {
        // Map: {"a"=1, "b"=2, "c"=3}
        ConcurrentNavigableMapNullSafe<String, Integer> base = new ConcurrentNavigableMapNullSafe<>();
        base.put("a", 1);
        base.put("b", 2);
        base.put("c", 3);
        // Map with a null key as well
        ConcurrentNavigableMapNullSafe<String, Integer> withNull = new ConcurrentNavigableMapNullSafe<>();
        withNull.put("a", 1);
        withNull.put("b", 2);
        withNull.put(null, 99);
        // Map without a null key
        ConcurrentNavigableMapNullSafe<String, Integer> withoutNull = new ConcurrentNavigableMapNullSafe<>();
        withoutNull.put("x", 10);
        withoutNull.put("y", 20);
        // Helper to inject the prepared map into the test instance via reflection
        return Stream.of(// key exists
        Arguments.of("b", "b", base), // key between existing keys
        Arguments.of("bb", "c", base), // key greater than any entry
        Arguments.of("z", null, base), // null key present
        Arguments.of(null, null, withNull), // null key absent – should return smallest real key
        Arguments.of(null, "a", base)).map(arg -> {
            String key = (String) arg.get()[0];
            Object expected = arg.get()[1];
            @SuppressWarnings("unchecked")
            ConcurrentNavigableMapNullSafe<String, Integer> map = (ConcurrentNavigableMapNullSafe<String, Integer>) arg.get()[2];
            return Arguments.of(key, expected, map);
        });
    }

    @Test
    void ceilingKey_basicOrdering() {
        // Given: a map with several ordered keys
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // When / Then: ceilingKey returns the least key greater than or equal to the argument
        // exact match
        assertEquals("a", map.ceilingKey("a"));
        assertEquals("b", map.ceilingKey("b"));
        assertEquals("c", map.ceilingKey("c"));
        // between "a" and "b"
        assertEquals("b", map.ceilingKey("aa"));
        // between "b" and "c"
        assertEquals("c", map.ceilingKey("bb"));
        // No key greater than "c"
        assertNull(map.ceilingKey("d"));
        // Key smaller than the smallest entry
        assertEquals("a", map.ceilingKey(""));
    }

    @Test
    void ceilingKey_withNullKeyPresent() {
        // Given: a map that also contains a null key (null is considered last)
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        // When: asking for ceilingKey of a normal key that is less than the null sentinel
        // exact match
        assertEquals("b", map.ceilingKey("b"));
        // "c" > "b" but null is considered after all non‑null keys
        assertEquals(null, map.ceilingKey("c"));
        // When: asking for ceilingKey of null itself
        // null key exists, so it is returned
        assertEquals(null, map.ceilingKey(null));
        // When: asking for a key smaller than the smallest entry
        assertEquals("a", map.ceilingKey(""));
    }

    @Test
    void ceilingKey_withoutNullKey() {
        // Given: a map without a null key
        map.put("x", 10);
        map.put("y", 20);
        // When: asking for a key greater than any existing key
        // no key >= "z"
        assertNull(map.ceilingKey("z"));
        // When: asking for a key that would be between existing keys
        assertEquals("y", map.ceilingKey("xz"));
    }

    @Test
    void ceilingKey_nullKeyAbsentReturnsNull() {
        // Given: a map that does NOT contain a null key
        map.put("a", 1);
        map.put("b", 2);
        // When: asking for ceilingKey(null) – null is considered last, but it is not present
        assertNull(map.ceilingKey(null));
    }

    @Test
    void ceilingKey_nullKeyWithComparator() {
        // Given: a map with a custom comparator that orders strings in reverse
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>((s1, s2) -> s2.compareTo(s1));
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        // null still considered last in the natural ordering view
        reverseMap.put(null, 99);
        // When: using the reverse comparator, ceilingKey works against the reversed order
        // In reverse order, "b" is the smallest, then "a", then null.
        // "c" > "b" in reverse order, so "b" is ceiling
        assertEquals("b", reverseMap.ceilingKey("c"));
        // after "a" comes null
        assertEquals(null, reverseMap.ceilingKey("a"));
        // null key itself
        assertEquals(null, reverseMap.ceilingKey(null));
    }

    @ParameterizedTest(name = "{index}: key={0} → expected={1}")
    @MethodSource("provideCeilingKeyBasicCases")
    void ceilingKey_basicCases(String key, Object expected) {
        // map is pre‑populated by the provider
        Object result = map.ceilingKey(key);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: key={0} → expected={1}")
    @MethodSource("provideCeilingKeyBasicCases")
    void ceilingKey_basicCases_injected(String key, Object expected, ConcurrentNavigableMapNullSafe<String, Integer> preparedMap) {
        // Replace the test instance's map with the prepared one.
        this.map = preparedMap;
        Object result = map.ceilingKey(key);
        assertEquals(expected, result);
    }

    @Test
    void ceilingKey_emptyMap_returnsNull() {
        assertNull(map.ceilingKey("any"));
    }

    @Test
    void ceilingKey_onlyNullEntry_returnsNullKey() {
        map.put(null, 0);
        assertNull(map.ceilingKey(null));
    }

    @Test
    void ceilingKey_onlyNullEntry_nonNullKeyReturnsNull() {
        map.put(null, 0);
        assertNull(map.ceilingKey("a"));
    }

    @Test
    void ceilingKey_smallestKeyReturned() {
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("b", map.ceilingKey("a"));
    }

    @Test
    void ceilingKey_integerKeys() {
        // Use Integer keys to verify numeric ordering.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(3, "three");
        assertEquals(3, intMap.ceilingKey(2));
    }

    @Test
    void ceilingKey_reverseComparator() {
        // Reverse order: c < b < a (natural order reversed)
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>((s1, s2) -> s2.compareTo(s1));
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        reverseMap.put(null, 99);
        // In reverse order "b" is the smallest key that is >= "bb"
        assertEquals("b", reverseMap.ceilingKey("bb"));
        // After "a" comes null in the reversed view
        assertNull(reverseMap.ceilingKey("a"));
        // Null key itself
        assertNull(reverseMap.ceilingKey(null));
    }

    @Test
    void ceilingKey_typeMismatch_throwsClassCastException() {
        // Map expects String keys; supplying an Integer should fail.
        map.put("a", 1);
        map.put("b", 2);
        assertThrows(ClassCastException.class, () -> map.ceilingKey((String) (Object) 5));
    }

    @Test
    void ceilingKey_nonComparableKey_throwsClassCastException() {
        map.put("a", 1);
        map.put("b", 2);
        // Supplying an Integer where String keys are expected.
        assertThrows(ClassCastException.class, () -> map.ceilingKey((String) (Object) 42));
    }

    @Test
    void ceilingKey_unrelatedObject_throwsClassCastException() {
        map.put("a", 1);
        map.put("b", 2);
        assertThrows(ClassCastException.class, () -> map.ceilingKey((String) (Object) new Object()));
    }

    @Test
    void ceilingKey_customComparatorBadNullHandling_throwsClassCastException() {
        // Comparator that blindly casts keys to String – will fail for the internal null sentinel.
        Comparator<String> badComparator = (k1, k2) -> ((String) k1).compareTo((String) k2);
        ConcurrentNavigableMapNullSafe<String, Integer> badMap = new ConcurrentNavigableMapNullSafe<>(badComparator);
        badMap.put(null, 0);
        // The sentinel object is not a String, so the comparator throws ClassCastException.
        assertThrows(ClassCastException.class, () -> badMap.ceilingKey(null));
    }

    @Test
    void ceilingKey_internalMapNull_throwsNullPointerException() throws Exception {
        // Using reflection to set the internalMap field to null.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.ceilingKey("any"));
    }
}
