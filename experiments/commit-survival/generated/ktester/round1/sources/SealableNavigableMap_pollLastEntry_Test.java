package com.cedarsoftware.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_pollLastEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple backing map – we avoid null keys for ordering clarity
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
    }

    @Test
    void pollLastEntry_Unsealed_ReturnsAndRemovesLastEntry() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        assertEquals(3, sealableMap.size());
        Map.Entry<String, Integer> last = sealableMap.pollLastEntry();
        // The natural ordering of String makes "c" the last key
        assertNotNull(last, "pollLastEntry should return the last entry");
        assertEquals("c", last.getKey());
        assertEquals(Integer.valueOf(3), last.getValue());
        // Entry must have been removed from the map
        assertEquals(2, sealableMap.size());
        assertFalse(sealableMap.containsKey("c"));
    }

    @Test
    void pollLastEntry_Sealed_ThrowsUnsupportedOperationException() {
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        assertThrows(UnsupportedOperationException.class, sealableMap::pollLastEntry, "pollLastEntry must throw when the map is sealed");
    }

    @Test
    void pollLastEntry_EmptyMap_ReturnsNull() {
        // Create an empty SealableNavigableMap
        NavigableMap<String, Integer> emptyBacking = new ConcurrentNavigableMapNullSafe<>();
        SealableNavigableMap<String, Integer> emptyMap = new SealableNavigableMap<>(emptyBacking, sealedSupplier);
        assertEquals(0, emptyMap.size());
        assertNull(emptyMap.pollLastEntry(), "pollLastEntry on an empty map should return null");
    }

    @Test
    void pollLastEntry_Unsealed_MultipleEntries_ReturnsAndRemovesLast() {
        // natural ordering of Integer → 2 is the last key
        backingMap = new ConcurrentSkipListMap<>();
        backingMap.put(0, "a");
        backingMap.put(1, "b");
        backingMap.put(2, "c");
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        assertEquals(3, sealableMap.size());
        Map.Entry<Integer, String> last = sealableMap.pollLastEntry();
        assertNotNull(last, "pollLastEntry should return the last entry");
        assertEquals(Integer.valueOf(2), last.getKey());
        assertEquals("c", last.getValue());
        // entry must have been removed
        assertEquals(2, sealableMap.size());
        assertFalse(sealableMap.containsKey(2));
    }

    @Test
    void pollLastEntry_Unsealed_EmptyMap_ReturnsNull() {
        backingMap = new ConcurrentSkipListMap<>();
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        assertEquals(0, sealableMap.size());
        assertNull(sealableMap.pollLastEntry(), "pollLastEntry on an empty map should return null");
    }

    @Test
    void pollLastEntry_Unsealed_SingleEntry_RemovesAndReturnsIt() {
        backingMap = new ConcurrentSkipListMap<>();
        backingMap.put(5, "single");
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        assertEquals(1, sealableMap.size());
        Map.Entry<Integer, String> entry = sealableMap.pollLastEntry();
        assertNotNull(entry);
        assertEquals(Integer.valueOf(5), entry.getKey());
        assertEquals("single", entry.getValue());
        // map must now be empty
        assertEquals(0, sealableMap.size());
        assertFalse(sealableMap.containsKey(5));
    }

    @Test
    void pollLastEntry_Sealed_NonEmpty_ThrowsIllegalStateException() {
        backingMap = new ConcurrentSkipListMap<>();
        backingMap.put(1, "x");
        backingMap.put(2, "y");
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        IllegalStateException ex = assertThrows(IllegalStateException.class, sealableMap::pollLastEntry, "pollLastEntry must throw when the map is sealed");
        assertEquals("Map is sealed", ex.getMessage());
        // underlying map must stay unchanged
        assertEquals(2, sealableMap.size());
    }

    @Test
    void pollLastEntry_Sealed_Empty_ThrowsIllegalStateException() {
        backingMap = new ConcurrentSkipListMap<>();
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        IllegalStateException ex = assertThrows(IllegalStateException.class, sealableMap::pollLastEntry, "Even an empty sealed map must throw");
        assertEquals("Map is sealed", ex.getMessage());
        assertEquals(0, sealableMap.size());
    }

    @Test
    void pollLastEntry_Sealed_RepeatedCalls_AlwaysThrow() {
        backingMap = new ConcurrentSkipListMap<>();
        backingMap.put(10, "ten");
        backingMap.put(20, "twenty");
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        sealed = true;
        for (int i = 0; i < 3; i++) {
            IllegalStateException ex = assertThrows(IllegalStateException.class, sealableMap::pollLastEntry, "Call #" + (i + 1) + " should still throw");
            assertEquals("Map is sealed", ex.getMessage());
        }
        // ensure map content is untouched
        assertEquals(2, sealableMap.size());
    }

    @Test
    void pollLastEntry_ReverseOrderComparator_ReturnsSmallestKey() {
        // reverse order → logical last is the smallest key (1)
        TreeMap<Integer, String> reverseMap = new TreeMap<>(Collections.reverseOrder());
        reverseMap.put(1, "a");
        reverseMap.put(2, "b");
        reverseMap.put(3, "c");
        sealableMap = new SealableNavigableMap<>(reverseMap, sealedSupplier);
        Map.Entry<Integer, String> entry = sealableMap.pollLastEntry();
        assertNotNull(entry);
        assertEquals(Integer.valueOf(1), entry.getKey());
        assertEquals("a", entry.getValue());
        // after removal the map should contain 2 entries (2 and 3)
        assertEquals(2, sealableMap.size());
        assertFalse(sealableMap.containsKey(1));
    }

    @Test
    void pollLastEntry_UnmodifiableUnderlyingMap_PropagatesUnsupportedOperationException() {
        NavigableMap<Integer, String> mutable = new ConcurrentSkipListMap<>();
        mutable.put(1, "one");
        mutable.put(2, "two");
        NavigableMap<Integer, String> unmodifiable = Collections.unmodifiableNavigableMap(mutable);
        sealableMap = new SealableNavigableMap<>(unmodifiable, sealedSupplier);
        // map is un‑sealed, so the wrapper will forward the call
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, sealableMap::pollLastEntry, "Underlying unmodifiable map should cause an exception to be propagated");
        // message is implementation‑specific; we only verify the type
        assertNotNull(ex);
    }
}
