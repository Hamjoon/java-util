package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_firstKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test method
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC; not strictly required
        map.clear();
    }

    /**
     * Supplies the test data for {@link #firstKeyStandardBehaviour}.
     */
    private static Stream<Arguments> provideStandardBehaviour() {
        return Stream.of(// Empty map – JDK contract is to throw NoSuchElementException
        Arguments.of("Empty map throws NoSuchElementException", (Runnable) (() -> {
            /* nothing */
        }), java.util.NoSuchElementException.class), // Single non‑null key
        Arguments.of("Single non‑null key returns that key", (Runnable) (() -> map.put("only", 1)), "only"), // Map contains only a null key – should return null after unmasking
        Arguments.of("Only null key returns null", (Runnable) (() -> map.put(null, 42)), null));
    }

    /**
     * Verify that {@code firstKey()} returns the smallest non‑null key.
     * Null keys are considered greater than any non‑null key, so they should not be returned.
     */
    @Test
    void firstKeyReturnsSmallestNonNullKey() {
        // given
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        // null key should be ignored for firstKey
        map.put(null, 99);
        // when
        String first = map.firstKey();
        // then
        assertEquals("a", first, "firstKey should return the lowest natural ordering key");
    }

    /**
     * Verify that when the map contains only a null key, {@code firstKey()} returns {@code null}.
     */
    @Test
    void firstKeyReturnsNullWhenOnlyNullKeyPresent() {
        // given
        map.put(null, 42);
        // when
        String first = map.firstKey();
        // then
        assertNull(first, "firstKey should return null when the only key present is null");
    }

    /**
     * Verify that {@code firstKey()} throws {@link NoSuchElementException}
     * when the map is empty, matching the contract of {@link ConcurrentNavigableMap#firstKey()}.
     */
    @Test
    void firstKeyThrowsOnEmptyMap() {
        // given: an empty map (setupBeforeEach already provides this)
        // when / then
        assertThrows(java.util.NoSuchElementException.class, map::firstKey, "firstKey should throw NoSuchElementException on an empty map");
    }

    /**
     * Parameterized test covering the three “normal” scenarios described in the
     * JSON test cases: empty map, single non‑null key, and map that contains only a null key.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideStandardBehaviour")
    void firstKeyStandardBehaviour(String description, Runnable setup, Object expected) {
        // given
        setup.run();
        // when / then
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, map::firstKey, "Expected exception for case: " + description);
        } else {
            // Expected a concrete return value (may be null)
            assertEquals(expected, map.firstKey(), "Unexpected firstKey() result for case: " + description);
        }
    }

    /**
     * Helper that replaces the internal {@code ConcurrentSkipListMap} with one that
     * uses the supplied comparator.  The map instance is the same, only its backing
     * store is swapped via reflection.
     */
    @SuppressWarnings("unchecked")
    private void setInternalMapWithComparator(Comparator<String> comparator) throws Exception {
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        ConcurrentSkipListMap<Object, Object> customMap = new ConcurrentSkipListMap<>((Comparator) comparator);
        internalMapField.set(map, customMap);
    }

    /**
     * Reverse‑order comparator: larger strings are considered “smaller”.
     * After inserting “a”, “b”, “c”, the firstKey() should be “c”.
     */
    @Test
    void firstKeyWithReverseComparator() throws Exception {
        // given – install a reverse order comparator
        setInternalMapWithComparator(Comparator.reverseOrder());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered greater than any non‑null key
        map.put(null, 0);
        // when
        String first = map.firstKey();
        // then
        assertEquals("c", first, "With a reverse comparator the smallest key should be the lexicographically largest string");
    }

    /**
     * Comparator that treats {@code null} as greater than any non‑null key.
     * After inserting {@code null} and “x”, the firstKey() should be “x”.
     */
    @Test
    void firstKeyWithNullGreaterComparator() throws Exception {
        // given – comparator that puts null last (i.e. greater)
        Comparator<String> nullLast = (k1, k2) -> {
            if (k1 == null && k2 == null)
                return 0;
            // null > any non‑null
            if (k1 == null)
                return 1;
            if (k2 == null)
                return -1;
            return k1.compareTo(k2);
        };
        setInternalMapWithComparator(nullLast);
        map.put(null, 100);
        map.put("x", 1);
        // when
        String first = map.firstKey();
        // then
        assertEquals("x", first, "Comparator treats null as greater, so firstKey() should return the smallest non‑null key");
    }

    /**
     * Force a {@link ClassCastException} by putting a key of an incompatible type
     * (Integer) into the underlying {@code ConcurrentSkipListMap}. The generic type of
     * {@code map} is {@code String}, so the call to {@code firstKey()} should fail.
     */
    @Test
    void firstKeyThrowsClassCastExceptionWhenKeyNotComparableToGeneric() throws Exception {
        // given – replace internal map with a raw ConcurrentSkipListMap
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        ConcurrentSkipListMap<Object, Object> rawMap = new ConcurrentSkipListMap<>();
        // Integer key, not compatible with String generic
        rawMap.put(123, 1);
        internalMapField.set(map, rawMap);
        // when / then
        assertThrows(ClassCastException.class, map::firstKey, "firstKey should propagate ClassCastException when internal map contains a non‑compatible key");
    }

    /**
     * Simulate a broken construction where {@code internalMap} is {@code null}.
     * Accessing it should result in a {@link NullPointerException}.
     */
    @Test
    void firstKeyThrowsNullPointerExceptionWhenInternalMapIsNull() throws Exception {
        // given – nullify the internal map via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // when / then
        assertThrows(NullPointerException.class, map::firstKey, "firstKey should throw NullPointerException when internalMap is null");
    }

    /**
     * Verify that an empty underlying {@code ConcurrentSkipListMap} still results in the
     * standard {@link java.util.NoSuchElementException} as per the {@link ConcurrentNavigableMap}
     * contract.
     */
    @Test
    void firstKeyThrowsNoSuchElementExceptionWhenUnderlyingMapEmpty() {
        // given – map is freshly created and empty (setupBeforeEach)
        // when / then
        assertThrows(java.util.NoSuchElementException.class, map::firstKey, "firstKey should throw NoSuchElementException when the underlying map is empty");
    }
}
