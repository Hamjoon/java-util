package com.cedarsoftware.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;

class SealableNavigableMap_lowerEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // populate a deterministic ordering map
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        backingMap.put("four", 4);
        // the SealableNavigableMap wraps the backing map
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /* --------------------------------------------------------------------- *
     *  Helper to build a simple immutable Map.Entry for expected values.    *
     * --------------------------------------------------------------------- */
    private static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    /**
     * Data provider for {@link #lowerEntryStandardBehaviour(Object, Object)}
     */
    static Stream<Arguments> standardBehaviourProvider() {
        return Stream.of(// group: lowerEntry - standard behavior
        // key exists
        Arguments.of(5, entry(3, "C")), // smallest key → null
        Arguments.of(1, null), // non‑existent key
        Arguments.of(4, entry(3, "C")), // null key → NPE
        Arguments.of(null, NullPointerException.class), // group: lowerEntry - additional edge cases
        // larger than any key
        Arguments.of(Integer.MAX_VALUE, entry(9, "I")), // smaller than smallest
        Arguments.of(-1, null), // existing key with predecessor
        Arguments.of(3, entry(2, "B")), // existing key with predecessor
        Arguments.of(2, entry(1, "A")), // equal to smallest (map starts at 1)
        Arguments.of(0, null), // Double key – we use a map that can compare Numbers
        Arguments.of(5.5, entry(5, "E")), // Incompatible type (String) → ClassCastException
        Arguments.of("5", ClassCastException.class));
    }

    /**
     * Data provider for {@link #lowerEntryComparatorExceptions(Object, Class)}
     */
    static Stream<Arguments> comparatorExceptionProvider() {
        return Stream.of(Arguments.of(5, IllegalArgumentException.class), Arguments.of(-10, IllegalStateException.class));
    }

    @Test
    void lowerEntryReturnsCorrectEntryWhenKeyIsInMap() {
        // "two" is present – the entry with the greatest key less than "two" is "one"
        Map.Entry<String, Integer> entry = sealableMap.lowerEntry("two");
        assertNotNull(entry, "lowerEntry should return a non‑null entry");
        assertEquals("one", entry.getKey());
        assertEquals(Integer.valueOf(1), entry.getValue());
        // key that does not exist but falls between existing keys
        entry = sealableMap.lowerEntry("two-and-half");
        assertNotNull(entry);
        assertEquals("two", entry.getKey());
        assertEquals(Integer.valueOf(2), entry.getValue());
    }

    @Test
    void lowerEntryReturnsNullWhenNoLowerKeyExists() {
        // "one" is the smallest key – there is no lower key
        assertNull(sealableMap.lowerEntry("one"));
        // also verify behavior for a key that is lexicographically before the smallest key
        assertNull(sealableMap.lowerEntry("aardvark"));
    }

    @Test
    void lowerEntryReturnsNullWhenMapIsEmpty() {
        // replace the backing map with an empty one
        backingMap.clear();
        // the SealableNavigableMap still wraps the same (now empty) map
        assertEquals(0, sealableMap.size());
        assertNull(sealableMap.lowerEntry("any"));
    }

    @Test
    void entryReturnedByLowerEntryIsUnmodifiableWhenSealed() {
        // use a simple integer→string map
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, sealedSupplier);
        // obtain a mutable entry first
        Map.Entry<Integer, String> entry = sut.lowerEntry(3);
        assertNotNull(entry);
        // seal the map – mutating the entry must now fail
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue("Z"), "setValue should be prohibited when the map is sealed");
    }

    @Test
    void entryReturnedByLowerEntryRemainsMutableWhenUnsealed() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, sealedSupplier);
        Map.Entry<Integer, String> entry = sut.lowerEntry(3);
        assertNotNull(entry);
        // map is still unsealed – setValue must succeed
        entry.setValue("Z");
        assertEquals("Z", sut.get(entry.getKey()));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests – standard behaviour and additional edge cases *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] lowerEntry({0}) → {1}")
    @MethodSource("standardBehaviourProvider")
    void lowerEntryStandardBehaviour(Object key, Object expected) {
        // ----- arrange ------------------------------------------------------
        // map with Integer keys 1‑9 and String values "A"‑"I"
        @SuppressWarnings("unchecked")
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");
        map.put(4, "D");
        map.put(5, "E");
        map.put(6, "F");
        map.put(7, "G");
        map.put(8, "H");
        map.put(9, "I");
        // wrap with SealableNavigableMap
        @SuppressWarnings("unchecked")
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, sealedSupplier);
        // for possible later inspection
        this.sealableMap = sut;
        // ----- act ---------------------------------------------------------
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // an exception is expected
            assertThrows((Class<? extends Throwable>) expected, () -> sut.lowerEntry((Integer) key));
        } else {
            @SuppressWarnings("unchecked")
            Map.Entry<Integer, String> actual = sut.lowerEntry((Integer) key);
            if (expected == null) {
                assertNull(actual, "Expected null for key " + key);
            } else {
                @SuppressWarnings("unchecked")
                Map.Entry<Integer, String> exp = (Map.Entry<Integer, String>) expected;
                assertNotNull(actual, "Expected a non‑null entry for key " + key);
                assertEquals(exp.getKey(), actual.getKey(), "Incorrect key returned for lowerEntry(" + key + ")");
                assertEquals(exp.getValue(), actual.getValue(), "Incorrect value returned for lowerEntry(" + key + ")");
            }
        }
    }

    /* --------------------------------------------------------------------- *
     *  Comparator‑exception propagation tests                              *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] lowerEntry({0}) throws {1}")
    @MethodSource("comparatorExceptionProvider")
    void lowerEntryComparatorExceptions(Object key, Class<? extends Throwable> expectedException) {
        // ----- arrange ------------------------------------------------------
        Comparator<Integer> throwingComparator = (a, b) -> {
            if (a == 5 || b == 5) {
                throw new IllegalArgumentException("Key 5 is illegal");
            }
            if (a < 0 || b < 0) {
                throw new IllegalStateException("Negative keys are illegal");
            }
            return Integer.compare(a, b);
        };
        NavigableMap<Integer, String> map = new TreeMap<>(throwingComparator);
        // populate with a few entries – they are not used because the comparator
        // throws before any comparison succeeds
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, sealedSupplier);
        // ----- act & assert -------------------------------------------------
        assertThrows(expectedException, () -> sut.lowerEntry((Integer) key));
    }

    /* --------------------------------------------------------------------- *
     *  Underlying map exception propagation test                             *
     * --------------------------------------------------------------------- */
    @Test
    void lowerEntryUnderlyingMapExceptionIsPropagated() {
        // ----- arrange ------------------------------------------------------
        @SuppressWarnings("unchecked")
        NavigableMap<Integer, String> mockMap = Mockito.mock(NavigableMap.class);
        Mockito.when(mockMap.lowerEntry(Mockito.anyInt())).thenThrow(new UnsupportedOperationException("lowerEntry not supported"));
        // other methods that might be called by SealableNavigableMap (size, etc.)
        Mockito.when(mockMap.size()).thenReturn(1);
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(mockMap, sealedSupplier);
        // ----- act & assert -------------------------------------------------
        assertThrows(UnsupportedOperationException.class, () -> sut.lowerEntry(42), "The exception from the underlying map must be propagated unchanged");
    }
}
