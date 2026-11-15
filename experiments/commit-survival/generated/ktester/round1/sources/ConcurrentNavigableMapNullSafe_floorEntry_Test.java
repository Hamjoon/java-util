package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_floorEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @FunctionalInterface
    private interface MapConfigurer<K, V> {

        void configure(ConcurrentNavigableMapNullSafe<K, V> map);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test and populate it with a deterministic set.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered the greatest key.
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no test leaks state.
        map.clear();
        map = null;
    }

    /**
     * Convenience factory for a map entry that can be compared with {@code assertEquals}.
     */
    private static Map.Entry<String, String> entry(String k, String v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    /**
     * Supplies arguments for the normal (non‑exceptional) test cases.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the key to query</li>
     *   <li>a {@code Consumer<ConcurrentNavigableMapNullSafe<String,String>>}
     *       that prepares the map (adds required entries)</li>
     *   <li>the expected result (either an {@code Entry} or {@code null})</li>
     * </ul>
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// exact match
        Arguments.of("apple", (MapConfigurer<String, String>) m -> m.put("apple", "red"), entry("apple", "red")), // lower key exists (apple < banana < cherry)
        Arguments.of("banana", (MapConfigurer<String, String>) m -> {
            m.put("apple", "red");
            m.put("cherry", "dark red");
        }, entry("apple", "red")), // key greater than all existing keys
        Arguments.of("zebra", (MapConfigurer<String, String>) m -> {
            m.put("apple", "red");
            m.put("cherry", "dark red");
        }, entry("cherry", "dark red")), // key smaller than smallest key
        Arguments.of("aardvark", (MapConfigurer<String, String>) m -> {
            m.put("apple", "red");
            m.put("cherry", "dark red");
        }, null), // null key present
        Arguments.of(null, (MapConfigurer<String, String>) m -> {
            m.put("apple", "red");
            m.put(null, "nullValue");
        }, entry(null, "nullValue")), // null key absent
        Arguments.of(null, (MapConfigurer<String, String>) m -> {
            m.put("apple", "red");
            m.put("cherry", "dark red");
        }, null));
    }

    @Test
    void floorEntry_exactKey() {
        Map.Entry<String, Integer> e = map.floorEntry("b");
        assertNotNull(e, "floorEntry should return an entry for an exact match");
        assertEquals("b", e.getKey());
        assertEquals(2, e.getValue());
    }

    @Test
    void floorEntry_betweenExistingKeys() {
        // "bb" is between "b" and "c"; the floor should be "b".
        Map.Entry<String, Integer> e = map.floorEntry("bb");
        assertNotNull(e);
        assertEquals("b", e.getKey());
        assertEquals(2, e.getValue());
        // "aa" is between "a" and "b"; the floor should be "a".
        e = map.floorEntry("aa");
        assertNotNull(e);
        assertEquals("a", e.getKey());
        assertEquals(1, e.getValue());
    }

    @Test
    void floorEntry_belowFirstKey() {
        // Anything less than the smallest key ("a") should yield null.
        assertNull(map.floorEntry("0"), "No entry should be returned for a key lower than the first entry");
        assertNull(map.floorEntry(""), "Empty string is lower than \"a\" in natural ordering");
    }

    @Test
    void floorEntry_withNullKey() {
        // Null is considered the greatest key, so the floor of null is the null entry itself.
        Map.Entry<String, Integer> e = map.floorEntry(null);
        assertNotNull(e);
        assertNull(e.getKey(), "The key of the returned entry should be null");
        assertEquals(99, e.getValue());
        // For a non‑null key that is less than the null sentinel, the floor should be the
        // greatest non‑null key (i.e., "c").
        e = map.floorEntry("z");
        assertNotNull(e);
        assertEquals("c", e.getKey());
        assertEquals(3, e.getValue());
    }

    @Test
    void floorEntry_onEmptyMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        assertNull(empty.floorEntry("any"), "Empty map must return null for any key");
        assertNull(empty.floorEntry(null), "Empty map must return null for null key");
    }

    @Test
    void floorEntry_withCustomComparator() {
        // Comparator that orders strings in reverse (so "c" < "b" < "a").
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>((k1, k2) -> k2.compareTo(k1));
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        // still the greatest key.
        reverseMap.put(null, 88);
        // In reverse order, the floor of "b" is "c".
        Map.Entry<String, Integer> e = reverseMap.floorEntry("b");
        assertNotNull(e);
        assertEquals("c", e.getKey());
        assertEquals(3, e.getValue());
        // The floor of "d" (greater than any existing key in reverse order) is "a".
        e = reverseMap.floorEntry("d");
        assertNotNull(e);
        assertEquals("a", e.getKey());
        assertEquals(1, e.getValue());
        // The floor of null is still the null entry.
        e = reverseMap.floorEntry(null);
        assertNotNull(e);
        assertNull(e.getKey());
        assertEquals(88, e.getValue());
    }

    @ParameterizedTest(name = "floorEntry({0}) → {2}")
    @MethodSource("normalCases")
    void floorEntry_normalScenarios(Object key, MapConfigurer<String, String> configurer, Map.Entry<String, String> expected) {
        // Arrange
        configurer.configure(map);
        // Act
        Map.Entry<String, String> result = map.floorEntry((String) key);
        // Assert
        if (expected == null) {
            assertNull(result, "Expected null for key: " + key);
        } else {
            assertNotNull(result, "Expected non‑null entry for key: " + key);
            assertEquals(expected.getKey(), result.getKey(), "Returned entry key does not match expected");
            assertEquals(expected.getValue(), result.getValue(), "Returned entry value does not match expected");
        }
    }

    /**
     * The underlying {@code internalMap} returns {@code null} because no floor entry exists.
     */
    @Test
    void floorEntry_internalMapReturnsNull() {
        // Map contains only "beta".  floorEntry("alpha") should be null.
        map.put("beta", "B");
        assertNull(map.floorEntry("alpha"), "When no key ≤ supplied key exists, floorEntry must return null");
    }

    /**
     * The internal map field is deliberately set to {@code null} to provoke NPE.
     */
    @Test
    void floorEntry_internalMapIsNull_throwsNPE() throws Exception {
        // Populate map so that the field exists.
        map.put("x", "X");
        // Use reflection to set the protected field 'internalMap' to null.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // The next call should throw NPE because the delegate is null.
        assertThrows(NullPointerException.class, () -> map.floorEntry("any"), "Calling floorEntry when internalMap is null must throw NullPointerException");
    }

    /**
     * Supplying a key of an incompatible type should raise {@code ClassCastException}.
     */
    @Test
    void floorEntry_incompatibleKeyType_throwsClassCastException() {
        map.put("apple", "red");
        // The map expects Strings; passing an Integer forces a comparison failure.
        assertThrows(ClassCastException.class, () -> map.floorEntry((String) (Object) Integer.valueOf(123)), "Incompatible key type must cause ClassCastException");
    }

    /**
     * Supplying a non‑Comparable key when natural ordering is used must raise {@code ClassCastException}.
     */
    @Test
    void floorEntry_nonComparableKey_throwsClassCastException() {
        map.put("apple", "red");
        // does not implement Comparable
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> map.floorEntry((String) nonComparable), "Non‑Comparable key must cause ClassCastException");
    }
}
