package com.cedarsoftware.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_firstEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // Initially unsealed.
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use the same concrete map implementation that the library provides.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // Create the SealableNavigableMap that wraps the backing map.
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    private static Stream<Arguments> basicBehaviorProvider() {
        // non‑empty map case
        NavigableMap<Integer, String> nonEmpty = new TreeMap<>();
        nonEmpty.put(1, "one");
        nonEmpty.put(2, "two");
        nonEmpty.put(3, "three");
        Map.Entry<Integer, String> expectedEntry = new AbstractMap.SimpleEntry<>(1, "one");
        // empty map case
        NavigableMap<Integer, String> empty = new TreeMap<>();
        return Stream.of(Arguments.of("When the underlying NavigableMap contains entries {1=one,2=two,3=three}, firstEntry() should return the entry with the smallest key (1).", nonEmpty, expectedEntry), Arguments.of("When the underlying NavigableMap is empty, firstEntry() must return null.", empty, null));
    }

    @Test
    void firstEntryReturnsLowestKeyWhenUnsealed() {
        Map.Entry<String, Integer> entry = sealableMap.firstEntry();
        // The natural ordering of strings makes "one" the smallest key.
        assertNotNull(entry, "firstEntry should not return null when map is non‑empty");
        assertEquals("one", entry.getKey(), "firstEntry should return the entry with the lowest key");
        assertEquals(Integer.valueOf(1), entry.getValue(), "Value should match the one associated with the lowest key");
    }

    @Test
    void firstEntryEntrySetValueThrowsWhenSealed() {
        // Obtain the entry before sealing – the entry object is still a live view.
        Map.Entry<String, Integer> entry = sealableMap.firstEntry();
        // Seal the map.
        sealed = true;
        // Attempting to modify the entry's value must fail.
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(99), "Modifying the value of an entry from a sealed map must throw UnsupportedOperationException");
    }

    @Test
    void firstEntryReflectsMutationsWhenUnsealed() {
        // Verify initial first entry.
        assertEquals("one", sealableMap.firstEntry().getKey());
        // Add a new entry that becomes the new lowest key.
        sealableMap.put("aardvark", 0);
        assertEquals("aardvark", sealableMap.firstEntry().getKey(), "firstEntry should reflect newly added entries that are lower in the ordering");
    }

    @Test
    void firstEntryRemainsImmutableWhenSealedEvenAfterFurtherMutationsAttempted() {
        // Seal the map.
        sealed = true;
        // The map is now read‑only; any attempt to put should throw.
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.put("zero", 0));
        // firstEntry should still return the original lowest entry.
        assertEquals("one", sealableMap.firstEntry().getKey(), "firstEntry should still return the original entry when the map is sealed");
    }

    /**
     * Group: FirstEntry - basic behavior
     *
     * Two scenarios:
     *   1) Map contains entries {1=one,2=two,3=three} → firstEntry returns key 1.
     *   2) Map is empty → firstEntry returns null.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicBehaviorProvider")
    void firstEntryBasicBehavior(String description, NavigableMap<Integer, String> map, Map.Entry<Integer, String> expected) {
        // Wrap the supplied map with a SealableNavigableMap (unsealed).
        Supplier<Boolean> dummySupplier = () -> false;
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, dummySupplier);
        Map.Entry<Integer, String> actual = sut.firstEntry();
        if (expected == null) {
            assertNull(actual, description);
        } else {
            assertNotNull(actual, description);
            assertEquals(expected.getKey(), actual.getKey(), description);
            assertEquals(expected.getValue(), actual.getValue(), description);
        }
    }

    /**
     * Group: FirstEntry - custom comparator (reverse order)
     *
     * A TreeMap with a descending comparator makes the highest key appear first.
     */
    @Test
    void firstEntryWithReverseComparator() {
        Comparator<Integer> reverse = Collections.reverseOrder();
        NavigableMap<Integer, String> reverseMap = new TreeMap<>(reverse);
        reverseMap.put(1, "one");
        reverseMap.put(2, "two");
        // highest key according to reverse order
        reverseMap.put(3, "three");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(reverseMap, () -> false);
        Map.Entry<Integer, String> entry = sut.firstEntry();
        assertNotNull(entry, "firstEntry should not be null for a non‑empty map");
        assertEquals(Integer.valueOf(3), entry.getKey(), "With a descending comparator the entry with the highest natural key (3) should be first");
        assertEquals("three", entry.getValue(), "Value must correspond to the key returned by firstEntry");
    }

    /**
     * Group: FirstEntry - null underlying map
     *
     * The internal navMap field is forced to null via reflection; calling firstEntry()
     * must throw a NullPointerException.
     */
    @Test
    void firstEntryThrowsWhenUnderlyingMapIsNull() throws Exception {
        // Construct a normal instance first.
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(backingMap, sealedSupplier);
        // Use reflection to set the private field 'navMap' to null.
        Field field = SealableNavigableMap.class.getDeclaredField("navMap");
        field.setAccessible(true);
        field.set(sut, null);
        // Expect NPE when firstEntry() is invoked.
        assertThrows(NullPointerException.class, sut::firstEntry, "If the internal navMap is null, firstEntry() should throw NullPointerException");
    }

    /**
     * Group: FirstEntry - concurrent modification
     *
     * The underlying map is mocked to throw ConcurrentModificationException when
     * firstEntry() is called, simulating a concurrent modification scenario.
     */
    @Test
    void firstEntryPropagatesConcurrentModificationException() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.firstEntry()).thenThrow(new ConcurrentModificationException("simulated concurrent modification"));
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(mockMap, () -> false);
        assertThrows(ConcurrentModificationException.class, sut::firstEntry, "firstEntry should propagate ConcurrentModificationException from the underlying map");
    }

    /**
     * Group: FirstEntry - map returns entry with null key
     *
     * TreeMap does not allow null keys unless a comparator that can handle nulls is supplied.
     * We provide such a comparator and verify that firstEntry() returns the null‑key entry.
     */
    @Test
    void firstEntryReturnsEntryWithNullKey() {
        Comparator<String> nullFriendly = (a, b) -> {
            if (a == b)
                return 0;
            // treat null as smallest
            if (a == null)
                return -1;
            if (b == null)
                return 1;
            return a.compareTo(b);
        };
        NavigableMap<String, String> map = new TreeMap<>(nullFriendly);
        map.put(null, "nullKeyValue");
        map.put("a", "valueA");
        map.put("b", "valueB");
        SealableNavigableMap<String, String> sut = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<String, String> entry = sut.firstEntry();
        assertNotNull(entry, "firstEntry should not be null when map contains a null key");
        assertNull(entry.getKey(), "The key of the first entry should be null");
        assertEquals("nullKeyValue", entry.getValue(), "The value associated with the null key must be returned correctly");
    }

    /**
     * Group: FirstEntry - map returns entry with null value
     *
     * Verify that a null value is preserved in the entry returned by firstEntry().
     */
    @Test
    void firstEntryReturnsEntryWithNullValue() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        // null value
        map.put(1, null);
        map.put(2, "two");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<Integer, String> entry = sut.firstEntry();
        assertNotNull(entry, "firstEntry should not be null when map is non‑empty");
        assertEquals(Integer.valueOf(1), entry.getKey(), "The smallest key (1) should be returned");
        assertNull(entry.getValue(), "The value associated with the key should be null as stored in the map");
    }
}
