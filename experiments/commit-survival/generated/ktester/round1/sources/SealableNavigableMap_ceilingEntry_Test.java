package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_ceilingEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test unsealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // use the same concrete implementation as the production tests
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("apple", 1);
        backingMap.put("banana", 2);
        backingMap.put("cherry", 3);
        // also test null handling
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear references to help GC; not strictly required
        backingMap = null;
        sealableMap = null;
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(// key exists
        Arguments.of("b", "b", 2), // key missing but higher key present
        Arguments.of("c", "d", 4), // key greater than any key
        Arguments.of("z", null, null), // key less than smallest key (should return first entry)
        Arguments.of("a", "a", 1), // empty map – any key yields null
        Arguments.of("any", null, null));
    }

    @Test
    void ceilingEntryReturnsExactMatchWhenKeyExists() {
        Map.Entry<String, Integer> entry = sealableMap.ceilingEntry("banana");
        assertNotNull(entry, "Ceiling entry for an existing key should not be null");
        assertEquals("banana", entry.getKey());
        assertEquals(Integer.valueOf(2), entry.getValue());
    }

    @Test
    void ceilingEntryReturnsNextHigherKeyWhenExactKeyMissing() {
        // "blueberry" is not present; the next higher key in natural order is "cherry"
        Map.Entry<String, Integer> entry = sealableMap.ceilingEntry("blueberry");
        assertNotNull(entry, "Ceiling entry for a missing key should return the next higher entry");
        assertEquals("cherry", entry.getKey());
        assertEquals(Integer.valueOf(3), entry.getValue());
    }

    @Test
    void ceilingEntryReturnsNullWhenKeyHigherThanAllEntries() {
        Map.Entry<String, Integer> entry = sealableMap.ceilingEntry("zoo");
        assertNull(entry, "Ceiling entry for a key greater than any map key should be null");
    }

    @Test
    void ceilingEntryHandlesNullKeyAccordingToNavigableMapContract() {
        // The underlying map is a ConcurrentNavigableMapNullSafe which permits null keys.
        // For a null key, ceilingEntry should return the entry for null itself.
        Map.Entry<String, Integer> entry = sealableMap.ceilingEntry(null);
        assertNotNull(entry, "Ceiling entry for null key should return the null-key entry");
        assertNull(entry.getKey());
        assertNull(entry.getValue());
    }

    @Test
    void ceilingEntryEntrySetValueThrowsWhenSealed() {
        // Retrieve an entry while unsealed, then seal the map and attempt to modify the entry.
        Map.Entry<String, Integer> entry = sealableMap.ceilingEntry("apple");
        assertNotNull(entry);
        // seal the map
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(99), "Modifying a map entry when the map is sealed must throw UnsupportedOperationException");
    }

    @Test
    void ceilingEntryWorksWhenMapIsSealedForReadOnlyAccess() {
        // seal the map
        sealed = true;
        // Reading should still be allowed
        Map.Entry<String, Integer> entry = sealableMap.ceilingEntry("banana");
        assertNotNull(entry);
        assertEquals("banana", entry.getKey());
        assertEquals(Integer.valueOf(2), entry.getValue());
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'' → expectedKey=''{1}''")
    @MethodSource("basicBehaviorProvider")
    void ceilingEntryBasicBehavior(String key, String expectedKey, Integer expectedValue) {
        // Build a map that matches the JSON description
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // For the “empty map” case we replace the map with an empty one
        if ("any".equals(key)) {
            map.clear();
        }
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        if (expectedKey == null) {
            assertNull(sut.ceilingEntry(key), "Expected null ceiling entry for key: " + key);
        } else {
            Map.Entry<String, Integer> entry = sut.ceilingEntry(key);
            assertNotNull(entry, "Ceiling entry should not be null for key: " + key);
            assertEquals(expectedKey, entry.getKey(), "Incorrect key returned");
            assertEquals(expectedValue, entry.getValue(), "Incorrect value returned");
        }
    }

    @Test
    void ceilingEntryNullKeyThrowsNullPointerException() {
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(NullPointerException.class, () -> sut.ceilingEntry(null), "Passing null key to a TreeMap‑backed SealableNavigableMap should throw NPE");
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryExactHighestKey() {
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<String, Integer> entry = sut.ceilingEntry("d");
        assertNotNull(entry);
        assertEquals("d", entry.getKey());
        assertEquals(Integer.valueOf(4), entry.getValue());
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryWithReverseComparator() {
        // Comparator that orders keys in descending (reverse) order
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableMap<String, Integer> map = new TreeMap<>(reverse);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        // In reverse order, the “ceiling” of "c" is the greatest key ≤ "c" in natural order,
        // which is "b".
        Map.Entry<String, Integer> entry = sut.ceilingEntry("c");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(Integer.valueOf(2), entry.getValue());
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryWithNonComparableKeyThrowsClassCastException() {
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        map.put("b", 2);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(ClassCastException.class, () -> sut.ceilingEntry((String) (Object) Integer.valueOf(123)), "Providing an Integer key to a String‑keyed map should throw ClassCastException");
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryWithNullValueEntry() {
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("e", null);
        map.put("f", 6);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<String, Integer> entry = sut.ceilingEntry("e");
        assertNotNull(entry);
        assertEquals("e", entry.getKey());
        assertNull(entry.getValue(), "The value associated with key 'e' should be null");
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryWithComparatorExpectingIntegerThrowsClassCastException() {
        Comparator<Object> intComparator = (o1, o2) -> {
            // Force a cast to Integer – will fail for non‑Integer arguments
            return Integer.compare((Integer) o1, (Integer) o2);
        };
        NavigableMap<Object, Integer> map = new TreeMap<>(intComparator);
        map.put(1, 10);
        map.put(2, 20);
        SealableNavigableMap<Object, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(ClassCastException.class, () -> sut.ceilingEntry("a"), "Comparator expects Integer keys; passing a String should cause ClassCastException");
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryComparatorThrowsIllegalArgumentException() {
        Comparator<String> explodingComparator = (k1, k2) -> {
            if ("bad".equals(k1) || "bad".equals(k2)) {
                throw new IllegalArgumentException("Bad key supplied");
            }
            return k1.compareTo(k2);
        };
        NavigableMap<String, Integer> map = new TreeMap<>(explodingComparator);
        map.put("good", 1);
        map.put("better", 2);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(IllegalArgumentException.class, () -> sut.ceilingEntry("bad"), "Comparator should propagate IllegalArgumentException thrown for key 'bad'");
    }

    // -----------------------------------------------------------------
    @Test
    void ceilingEntryUnderlyingMapThrowsUnsupportedOperationException(@Mock NavigableMap<String, Integer> mockMap) {
        // Configure the mock to throw when ceilingEntry is invoked
        when(mockMap.ceilingEntry(anyString())).thenThrow(new UnsupportedOperationException("Not supported"));
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(mockMap, () -> false);
        assertThrows(UnsupportedOperationException.class, () -> sut.ceilingEntry("any"), "The UnsupportedOperationException from the underlying map must propagate");
    }
}
