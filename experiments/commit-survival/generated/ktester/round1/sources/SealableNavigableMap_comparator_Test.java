package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_comparator_Test {

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    private SealableNavigableMap<String, Integer> sealableMap;

    @Nested
    @DisplayName("Comparator Retrieval – Propagation of RuntimeException from underlying map")
    class ExceptionPropagationTests {

        @Test
        @DisplayName("Underlying map throws IllegalStateException")
        void comparatorPropagatesIllegalStateException() {
            @SuppressWarnings("unchecked")
            NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
            when(mockMap.comparator()).thenThrow(new IllegalStateException("boom"));
            SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mockMap, sealedSupplier);
            IllegalStateException ex = assertThrows(IllegalStateException.class, map::comparator, "IllegalStateException should propagate unchanged");
            assertEquals("boom", ex.getMessage());
        }

        @Test
        @DisplayName("Underlying map throws UnsupportedOperationException")
        void comparatorPropagatesUnsupportedOperationException() {
            @SuppressWarnings("unchecked")
            NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
            when(mockMap.comparator()).thenThrow(new UnsupportedOperationException("not supported"));
            SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mockMap, sealedSupplier);
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, map::comparator, "UnsupportedOperationException should propagate unchanged");
            assertEquals("not supported", ex.getMessage());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default the map is unsealed; individual tests can change this flag
        sealed = false;
        sealedSupplier = () -> sealed;
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and avoid cross‑test contamination
        sealableMap = null;
    }

    @Test
    void comparatorReturnsUnderlyingComparator() {
        // Arrange – a map with a custom (reverse) comparator
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        NavigableMap<String, Integer> underlying = new TreeMap<>(reverseOrder);
        underlying.put("a", 1);
        underlying.put("b", 2);
        sealableMap = new SealableNavigableMap<>(underlying, sealedSupplier);
        // Act
        Comparator<? super String> result = sealableMap.comparator();
        // Assert – the comparator returned by SealableNavigableMap is the same instance
        assertSame(reverseOrder, result, "Comparator should be delegated to the wrapped map");
    }

    @Test
    void comparatorReturnsNullWhenNaturalOrdering() {
        // Arrange – a map that relies on natural ordering (no explicit comparator)
        NavigableMap<String, Integer> underlying = new TreeMap<>();
        underlying.put("a", 1);
        underlying.put("b", 2);
        sealableMap = new SealableNavigableMap<>(underlying, sealedSupplier);
        // Act
        Comparator<? super String> result = sealableMap.comparator();
        // Assert – natural ordering means the comparator is null
        assertNull(result, "Comparator should be null for a naturally ordered map");
    }

    @Test
    void comparatorUnaffectedBySealedState() {
        // Arrange – a map with a custom comparator
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        NavigableMap<String, Integer> underlying = new TreeMap<>(caseInsensitive);
        sealableMap = new SealableNavigableMap<>(underlying, sealedSupplier);
        // Act & Assert – first when unsealed
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        assertSame(caseInsensitive, sealableMap.comparator(), "Comparator should be the same regardless of sealed state (unsealed)");
        // Change sealed state and verify again
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should now be sealed");
        assertSame(caseInsensitive, sealableMap.comparator(), "Comparator should be the same regardless of sealed state (sealed)");
    }

    @Test
    @DisplayName("Comparator Retrieval – Natural Ordering on ConcurrentSkipListMap")
    void comparatorReturnsNullForConcurrentSkipListMapNaturalOrder() {
        // ConcurrentSkipListMap without an explicit comparator uses natural ordering
        NavigableMap<Integer, String> underlying = new ConcurrentSkipListMap<>();
        underlying.put(1, "one");
        underlying.put(2, "two");
        // Use raw types for simplicity – the generic type of SealableNavigableMap is irrelevant here
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(underlying, sealedSupplier);
        assertNull(map.comparator(), "Comparator should be null when the underlying ConcurrentSkipListMap uses natural ordering");
    }

    @Test
    @DisplayName("Comparator Retrieval – Custom String Comparator (case‑insensitive)")
    void comparatorReturnsCustomStringComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        NavigableMap<String, Integer> underlying = new TreeMap<>(caseInsensitive);
        underlying.put("Apple", 1);
        underlying.put("banana", 2);
        sealableMap = new SealableNavigableMap<>(underlying, sealedSupplier);
        Comparator<? super String> result = sealableMap.comparator();
        assertSame(caseInsensitive, result, "The same case‑insensitive Comparator instance should be returned");
    }

    @Test
    @DisplayName("Comparator Retrieval – Comparator Identity Preservation")
    void comparatorPreservesIdentity() {
        Comparator<Integer> reverseInt = Comparator.reverseOrder();
        NavigableMap<Integer, String> underlying = new TreeMap<>(reverseInt);
        underlying.put(10, "ten");
        underlying.put(5, "five");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(underlying, sealedSupplier);
        // The returned comparator must be the exact same object (identity), not just an equal copy
        assertSame(reverseInt, map.comparator(), "Comparator identity must be preserved when delegating to the backing map");
    }

    @Test
    @DisplayName("Comparator Retrieval – Comparator After Map Modification")
    void comparatorRemainsSameAfterMutations() {
        Comparator<Integer> reverseInt = Comparator.reverseOrder();
        NavigableMap<Integer, String> underlying = new TreeMap<>(reverseInt);
        // Populate with several entries
        for (int i = 0; i < 20; i++) {
            underlying.put(i, "val" + i);
        }
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(underlying, sealedSupplier);
        // Mutate the map further
        underlying.put(100, "hundred");
        underlying.remove(5);
        // Comparator reference must still be the original instance
        assertSame(reverseInt, map.comparator(), "Comparator reference must not change after modifications to the map");
    }

    @Test
    @DisplayName("Comparator Retrieval – Null Comparator from Custom NavigableMap Implementation")
    void comparatorPropagatesNullFromCustomImplementation() {
        // Mock a NavigableMap whose comparator() returns null
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.comparator()).thenReturn(null);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mockMap, sealedSupplier);
        assertNull(map.comparator(), "SealableNavigableMap should propagate a null comparator from the underlying map");
    }

    @Test
    @DisplayName("Comparator Retrieval – NullPointerException when backing map is null")
    void comparatorThrowsNPEWhenBackingMapIsNull() throws Exception {
        // Use reflection to invoke the public constructor with a null backing map.
        // The class is expected to store the argument directly in the final field `navMap`.
        // Calling comparator() should therefore dereference a null field and throw NPE.
        var ctor = SealableNavigableMap.class.getDeclaredConstructor(NavigableMap.class, Supplier.class);
        ctor.setAccessible(true);
        @SuppressWarnings("unchecked")
        SealableNavigableMap<String, Integer> map = (SealableNavigableMap<String, Integer>) ctor.newInstance(null, sealedSupplier);
        assertThrows(NullPointerException.class, map::comparator, "Calling comparator() when the backing map is null must throw NullPointerException");
    }
}
